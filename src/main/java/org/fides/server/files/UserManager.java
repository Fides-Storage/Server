package org.fides.server.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.Security;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Actions;
import org.fides.encryption.EncryptionUtils;
import org.fides.encryption.KeyGenerator;
import org.fides.server.tools.CommunicationUtil;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;
import org.fides.server.tools.PropertiesManager;
import org.fides.server.tools.UserLocker;
import org.fides.tools.HashUtils;

import com.google.gson.JsonObject;

/**
 * This class manages the users using static functions. It can unlock and save user files.
 */
public final class UserManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UserManager.class);

	// Add the Bouncycastle provider
	static {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	/** Size of the salt used in generating the master key, it should NEVER change */
	private static final int SALT_SIZE = 16; // 128 bit

	/**
	 * Opens the user file based on the user name and decrypts it based on the password hash
	 * 
	 * @param username
	 *            the given user name
	 * @param passwordHash
	 *            the given password hash
	 * @return the user file
	 */
	public static UserFile unlockUserFile(String username, String passwordHash) {
		File file = new File(PropertiesManager.getInstance().getUserDir(), username);

		UserFile userFile = null;
		DataInputStream din = null;
		ObjectInputStream inDecrypted = null;
		InputStream in = null;
		// Check if the username is in the folder and if the file isn't locked
		if (checkIfUserExists(username) && UserLocker.lock(username)) {
			try {
				in = new FileInputStream(file);
				din = new DataInputStream(in);

				// Get salt and amount of rounds from the beginning of the file
				byte[] saltBytes = new byte[SALT_SIZE];
				int pbkdf2Rounds = din.readInt();
				din.read(saltBytes, 0, SALT_SIZE);

				// Generate Key bases on the users password
				Key key = KeyGenerator.generateKey(passwordHash, saltBytes, pbkdf2Rounds, EncryptionUtils.KEY_SIZE);

				// Create the DecryptionStream
				inDecrypted = new ObjectInputStream(EncryptionUtils.getDecryptionStream(din, key));

				// Read the UserFile from the DecryptionStream
				userFile = (UserFile) inDecrypted.readObject();

				// Validate the password
				if (userFile.checkPasswordHash(passwordHash)) {
					return userFile;
				}

			} catch (FileNotFoundException e) {
				log.debug("UserFile not found for username: " + username);
			} catch (IOException e) {
				log.error("IOException has occured", e);
			} catch (ClassNotFoundException e) {
				log.error("UserFile was not a UserFile", e);
			} finally {
				// If the userfile couldn't be loaded, the user should be unlocked.
				if (userFile == null) {
					UserLocker.unlock(username);
				}
				IOUtils.closeQuietly(inDecrypted);
				IOUtils.closeQuietly(din);
				IOUtils.closeQuietly(in);
			}
		}
		return null;
	}

	/**
	 * Encrypts the user file and saves it in the user directory
	 * 
	 * @param userFile
	 *            the user file based on the user name
	 * @return true if succeeded, false otherwise
	 */
	public static boolean saveUserFile(UserFile userFile) {
		FileOutputStream fos = null;
		DataOutputStream dout = null;
		OutputStream outEncrypted = null;
		try {
			File userFileLocation = new File(PropertiesManager.getInstance().getUserDir(), userFile.getUsernameHash());

			if (userFileLocation.getName().equals(userFile.getUsernameHash())) {
				fos = new FileOutputStream(userFileLocation);
				dout = new DataOutputStream(fos);

				// Get salt and amount of rounds
				byte[] saltBytes = KeyGenerator.getSalt(SALT_SIZE);
				int pbkdf2Rounds = KeyGenerator.getRounds();

				// Generate a Key bases on the user's password
				Key key = KeyGenerator.generateKey(userFile.getPasswordHash(), saltBytes, pbkdf2Rounds, EncryptionUtils.KEY_SIZE);

				// Write the salt and amount of rounds to the beginning of the file
				dout.writeInt(pbkdf2Rounds);
				dout.write(saltBytes, 0, SALT_SIZE);

				// Create an encryptionstream
				outEncrypted = EncryptionUtils.getEncryptionStream(dout, key);
				ObjectOutputStream objectOut = new ObjectOutputStream(outEncrypted);
				objectOut.writeObject(userFile);

				// Flush all streams
				outEncrypted.flush();
				dout.flush();
				fos.flush();

				return true;
			}

		} catch (FileNotFoundException e) {
			log.error("UserFile not found", e);
		} catch (IOException e) {
			log.error("IOException has occured", e);
		} finally {
			IOUtils.closeQuietly(outEncrypted);
			IOUtils.closeQuietly(dout);
			IOUtils.closeQuietly(fos);
		}

		return false;
	}

	/**
	 * Checks if user name exists
	 * 
	 * @param username
	 *            the given user name
	 * @return whether the username exists or not
	 */
	public static boolean checkIfUserExists(String username) {
		File userFile = new File(PropertiesManager.getInstance().getUserDir(), username);

		// Check if username is in the folder
		return userFile.exists() && userFile.getName().equals(username) && userFile.isFile();
	}

	/**
	 * Creates a user based on received json object
	 * 
	 * @param userObject
	 *            jsonObject containing username and password
	 * @param out
	 *            outputstream to the client
	 * @throws IOException
	 *             if failed to write to outputstream
	 */
	public static void createUser(JsonObject userObject, DataOutputStream out) throws IOException {
		String usernameHash = HashUtils.hash(JsonObjectHandler.getProperty(userObject, Actions.Properties.USERNAME_HASH));
		String passwordHash = JsonObjectHandler.getProperty(userObject, Actions.Properties.PASSWORD_HASH);

		if (StringUtils.isNotBlank(usernameHash) && StringUtils.isNotBlank(passwordHash)) {
			if (UserManager.checkIfUserExists(usernameHash)) {
				CommunicationUtil.returnError(out, Errors.USNERNAME_EXISTS);

			} else {
				UserFile uf = new UserFile(usernameHash, passwordHash);
				if (UserManager.saveUserFile(uf)) {
					CommunicationUtil.returnSuccessful(out);
				} else {
					CommunicationUtil.returnError(out, Errors.CANNOT_SAVE_USER_FILE);
				}
			}
		} else {
			CommunicationUtil.returnError(out, Errors.USERNAME_OR_PASSWORD_EMPTY);
		}

	}

	/**
	 * Authenticate user based on jsonobject with username and password
	 * 
	 * @param userObject
	 *            json object with at least username and password
	 * @param out
	 *            output stream to client to write error message
	 * @return if user is authenticated or not
	 */
	public static UserFile authenticateUser(JsonObject userObject, DataOutputStream out) throws IOException {
		UserFile userFile = null;
		String usernameHash = HashUtils.hash(JsonObjectHandler.getProperty(userObject, Actions.Properties.USERNAME_HASH));
		String passwordHash = JsonObjectHandler.getProperty(userObject, Actions.Properties.PASSWORD_HASH);

		if (StringUtils.isNotBlank(usernameHash) && StringUtils.isNotBlank(passwordHash)) {
			if (!UserLocker.isLocked(usernameHash)) {
				userFile = UserManager.unlockUserFile(usernameHash, passwordHash);

				if (userFile != null) {
					log.trace("AuthenticateUser Successful");
					CommunicationUtil.returnSuccessful(out);
				} else {
					log.error(Errors.USERNAME_OR_PASSWORD_INCORRECT);
					CommunicationUtil.returnError(out, Errors.USERNAME_OR_PASSWORD_INCORRECT);
				}
			} else {
				log.error(Errors.SERVER_CANNOT_RESPOND);
				CommunicationUtil.returnError(out, Errors.SERVER_CANNOT_RESPOND);
			}
		} else {
			log.error(Errors.USERNAME_OR_PASSWORD_EMPTY);
			CommunicationUtil.returnError(out, Errors.USERNAME_OR_PASSWORD_EMPTY);
		}

		return userFile;
	}

}

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.encryption.EncryptionUtils;
import org.fides.encryption.KeyGenerator;
import org.fides.server.tools.PropertiesManager;

/**
 * This class manages the users using static functions. It can unlock and save user files.
 *
 * @author Niels and Jesse
 */
public final class UserManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UserManager.class);

	static{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	/**
	 * Opens the user file based on the user name and decrypts it based on the password hash
	 *
	 * @param username     the given user name
	 * @param passwordHash the given password hash
	 * @return the user file
	 */
	public static UserFile unlockUserFile(String username, String passwordHash) {
		File file = new File(PropertiesManager.getInstance().getUserDir(), username);

		DataInputStream din = null;
		ObjectInputStream inDecrypted = null;
		InputStream in = null;
		// Check if the username is in the folder
		if (checkIfUserExists(username)) {
			ObjectInputStream userFileObject = null;
			try {
				in = new FileInputStream(file.getPath());
				din = new DataInputStream(in);

				byte[] saltBytes = new byte[EncryptionUtils.SALT_SIZE];
				int pbkdf2Rounds = din.readInt();
				din.read(saltBytes, 0, EncryptionUtils.SALT_SIZE);

				Key key = KeyGenerator.generateKey(passwordHash, saltBytes, pbkdf2Rounds, EncryptionUtils.KEY_SIZE);

				inDecrypted = new ObjectInputStream(EncryptionUtils.getDecryptionStream(din, key));

				UserFile userFile = (UserFile) inDecrypted.readObject();
				// TODO: decrypt file

				//userFileObject = new ObjectInputStream(in);
				//UserFile userFile = (UserFile) userFileObject.readObject();

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
				IOUtils.closeQuietly(userFileObject);
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
	 * @param userFile the user file based on the user name
	 * @return true if succeeded, false otherwise
	 */
	public static boolean saveUserFile(UserFile userFile) {
		FileOutputStream fos = null;
		DataOutputStream dout = null;
			OutputStream outEncrypted = null;
		try {
			File userFileLocation = new File(PropertiesManager.getInstance().getUserDir(), userFile.getUsername());

			if (userFileLocation.getName().equals(userFile.getUsername())) {
				fos = new FileOutputStream(userFileLocation);
				dout = new DataOutputStream(fos);

				byte[] saltBytes = KeyGenerator.getSalt(EncryptionUtils.SALT_SIZE);
				int pbkdf2Rounds = KeyGenerator.getRounds();

				Key key = KeyGenerator.generateKey(userFile.getPassword(), saltBytes, pbkdf2Rounds, EncryptionUtils.KEY_SIZE);

				dout.writeInt(pbkdf2Rounds);
				dout.write(saltBytes, 0, EncryptionUtils.SALT_SIZE);

				outEncrypted = EncryptionUtils.getEncryptionStream(dout, key);
				ObjectOutputStream objectOut = new ObjectOutputStream(outEncrypted);
				objectOut.writeObject(userFile);
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
	 * @param username the given user name
	 * @return username exists or not
	 */
	public static boolean checkIfUserExists(String username) {
		File userFile = new File(PropertiesManager.getInstance().getUserDir(), username);

		// Check if username is in the folder
		return userFile.exists() && userFile.getName().equals(username) && userFile.isFile();
	}

}

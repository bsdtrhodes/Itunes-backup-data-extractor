/*-
 * Copyright (c) 2025 Tom Rhodes. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package me.bsdtrhodes.iExtractor;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * The manager class will actually "manage" various aspects
 * of this tool such as creating directories and extracting
 * files or other data from the backup files. Many methods
 * here are also "helpers" to perform actions in place of
 * being bundled with their specific object.
 * 
 * NOTES:
 * - Move some more things to the Window Manager.
 *
 */
public class BackupManager extends Phone {

	private Path backupLocation, restoreLocation;
	private Phone iPhone = null;

	/* The files we are working with. */
    private Path manifestDBFile, decryptedDatabaseFile;

	/* These lists are created by the manager from databases. */
	private ArrayList<Media> MediaList = new ArrayList<>();
	private ArrayList<Contact> ContactList = new ArrayList<>();
	private ArrayList<VoiceMail> VMailList = new ArrayList<>();
	private ArrayList<Message> MessageList = new ArrayList<>();
	private ArrayList<SafariHistory> SafariHistoryList =
			new ArrayList<SafariHistory>();
	private ArrayList<PhoneCall> callHistoryList = new ArrayList<>();

	/* Dictionary files that the iPhone object will need. */
	private NSDictionary infoPList, manifestPList;

	/* Cleanup queue. */
	private final CleanupManager cleanupManager;

	/*
	 * Is encrypted is true until we know otherwise; isLegit is only true
	 * when instantiated from a good backup, allowing certain processes
	 * to proceed or be blocked until available.
	 */
	public boolean phoneIsEncrypted = true;

	/* We set this if the user would like a final report of the extraction. */
	private boolean doReporting = false;

	/* So we can use self and save some typing. */
	final BackupManager self = BackupManager.this;

	/* Null backup object. */
	public BackupManager() throws ExceptionManager {
		/* Allow for a basic, null Backup Manager object. */
		try {
			this.cleanupManager = new CleanupManager();
		} catch (IOException e) {
			throw new ExceptionManager("Failed to delete temporary files: ",
					e, true);
		}
	}

	/* Constructor, once called, everything is built. */
    public BackupManager(Path backupLocation, Path restoreLocation,
    		boolean report) throws ExceptionManager, SQLException {

    	/* Set some needed globals. */
    	this.backupLocation = backupLocation;
    	this.restoreLocation = restoreLocation;
    	this.doReporting = report;

    	/* Now, register the shutdown cleanup hook here: */
    	try {
    		this.cleanupManager = new CleanupManager();
    	} catch (IOException e) {
    		throw new ExceptionManager("Failed to delete temporary files: ",
    				e, true);
    	}
    }

/*
 * Provide a method to "do the actual extraction."
 */
	public void doAllWorkAsync(Consumer<Boolean> onFinished)
		throws ExceptionManager, NoSuchAlgorithmException,
		IOException, InvalidKeyException {

		this.manifestDBFile = FilePairManager.MANIFEST
			.getEncryptedPath(backupLocation);

	/* This check should never be reached in a normal situation. */
	if (Files.exists(this.manifestDBFile))
		WindowManager.critical(ContextManager.getPrimaryStage(),
				"Critical Error: Potential data loss!",
				"Refusing to overwrite existing database, please be"
				+ " sure the restore directory is empty or the correct"
				+ " location was selected. Exiting.");

    /* Parse the property list files for the phone object. */
    try {
		this.infoPList = (NSDictionary)
			PropertyListParser.parse(this.backupLocation
			.resolve("info.plist").toFile());
		this.manifestPList = (NSDictionary)
			PropertyListParser.parse(this.backupLocation
			.resolve("Manifest.plist").toFile());
	} catch (ParserConfigurationException | ParseException | SAXException
			| PropertyListFormatException
			| IOException e) {
		throw new ExceptionManager("Critical error reading primary files: ",
				e, true);
	}

    /*
	 * Show a little "working" box.
	 */
	Stage BMStage = new Stage();
	ProgressIndicator PI = new ProgressIndicator();
	StackPane root = new StackPane();
	root.getChildren().add(PI);
	Scene scene = new Scene(root, 400, 300);
	BMStage.setScene(scene);
	BMStage.setTitle("Working ... Please wait!");
	ContextManager.getPrimaryStage();
	BMStage.initModality(Modality.APPLICATION_MODAL);
	BMStage.show();

	Task<Void> backgroundTask = new Task<>() {
        @Override
        protected Void call() throws Exception {
            updateProgress(0, 1);

	/* Set up a new iPhone. */
	self.iPhone = new Phone(self.infoPList, self.manifestPList);

	if (iPhone.isEncrypted) {
		/* Try to unlock the phone and process encrypted backup. */
		tryUnlock();
		phoneIsEncrypted = true;

		self.decryptedDatabaseFile = FilePairManager.MANIFEST
				.getDecryptedPath(self.restoreLocation);
		/*
		 * If the phone was unlocked, set the decrypted file to the
		 * manifest file.
		 */
		if (! self.iPhone.isEncrypted) self.decryptedDatabaseFile =
				FilePairManager.MANIFEST
				.getDecryptedPath(self.restoreLocation);

	} else {
		self.decryptedDatabaseFile = self.manifestDBFile;
		phoneIsEncrypted = false;
		copyUnecryptedFiles();
	}
	updateProgress(0.50f, 1.0f);
	try {
		if (iPhone.isEncrypted) {

		/* Attempt to perform decryption activities */
		EncryptedFile contactsDB = new
				EncryptedFile(FilePairManager.CONTACTS
				.getEncryptedPath(self.backupLocation),
				self.decryptedDatabaseFile, self.iPhone);
		contactsDB.extract(FilePairManager.CONTACTS
				.getDecryptedPath(self.restoreLocation));

		EncryptedFile txtMessagesDB = new
			EncryptedFile(FilePairManager.MESSAGES
				.getEncryptedPath(self.backupLocation),
				self.decryptedDatabaseFile,
				self.iPhone);
		txtMessagesDB.extract(FilePairManager.MESSAGES
				.getDecryptedPath(self.restoreLocation));

		EncryptedFile voicemailsDB = new
				EncryptedFile(FilePairManager.VOICEMAILS
				.getEncryptedPath(self.backupLocation),
				self.decryptedDatabaseFile,
				self.iPhone);
		voicemailsDB.extract(FilePairManager.VOICEMAILS
				.getDecryptedPath(self.restoreLocation));


		EncryptedFile safariHistoryDB = new
				EncryptedFile(FilePairManager.SAFARI
				.getEncryptedPath(self.backupLocation),
				self.decryptedDatabaseFile, self.iPhone);
		safariHistoryDB.extract(FilePairManager.SAFARI
				.getDecryptedPath(self.restoreLocation));

		EncryptedFile callLog = new
				EncryptedFile(FilePairManager.CALLS
				.getEncryptedPath(self.backupLocation),
				self.decryptedDatabaseFile, self.iPhone);
		callLog.extract(FilePairManager.CALLS
				.getDecryptedPath(self.restoreLocation));

		/* XXX: Get XML data for the report. - Move to reporting.
		File myPlist = new File(backupLocation + File.separator
				+ "Manifest.plist");
		File XMLFile = new File(restoreLocation
				+ File.separator + "ReportContents.xml");

		try {
			PropertyListParser.convertToXml(myPlist, XMLFile);
		} catch (ParserConfigurationException | ParseException |
				SAXException | PropertyListFormatException
				| IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */

		} else {
			/* Could probably just be a different "new" manager. */
			self.decryptedDatabaseFile = self.manifestDBFile;
		}

		/* Register files for cleanup */
		if (! ContextManager.getDebug()) {
			cleanupManager.markForCleanup(FilePairManager.CONTACTS
			.getDecryptedPath(self.restoreLocation));
		}

		if (! ContextManager.getDebug()) {
			cleanupManager.markForCleanup(FilePairManager.MESSAGES
			.getDecryptedPath(self.restoreLocation));
		}

		if (! ContextManager.getDebug()) {
			cleanupManager.markForCleanup(FilePairManager.VOICEMAILS
			.getDecryptedPath(self.restoreLocation));
		}

		if (! ContextManager.getDebug()) {
			cleanupManager.markForCleanup(FilePairManager.SAFARI
			.getDecryptedPath(self.restoreLocation));
		}

		if (! ContextManager.getDebug()) {
			cleanupManager.markForCleanup(FilePairManager.CALLS
			.getDecryptedPath(self.restoreLocation));
		}

		/* Finally, register the main database (Manifest) for cleanup. */
		if (! ContextManager.getDebug()) {
			cleanupManager.markForCleanup(FilePairManager.MANIFEST
			.getDecryptedPath(self.restoreLocation));
		}

		/* Create extraction directory hierarchy. */
		self.createMediaHier();

		/* Create the media file array. */
		MediaList = self.createMediaArray();
		updateProgress(0.75f, 1.0f);
		/*
		 * Read only to DB, separate connection, try-with-resources,
		 * no read lock should be needed here IIRC.
		 */
		try {
			ThreadManager workers = new ThreadManager(6);
			/* Create and write contact list to file. */
			Runnable contactTask = () -> {
				try {
					ContactList = self.createContactArray();
				} catch (ExceptionManager | SQLException e) {
					catchHelper("Error reading contacts", e, true);
				}
				try {
					self.writeContactsWeb(ContactList);
				} catch (ExceptionManager | IOException e) {
					catchHelper("Error reading contacts", e, true);
				}
				if (self.doReporting) {
					try {
						self.writeContactsToFile(ContactList);
					} catch (ExceptionManager e) {
						catchHelper("Error reading contacts", e, true);
					}
				}
			};
			workers.submitJob(contactTask, 0);

			/* Create and write call history to file. */
			Runnable callHistoryTask = () -> {
				try {
					callHistoryList = self.createCallHistory();
				} catch (ExceptionManager e) {
					catchHelper("Error reading contacts", e, true);
				}
				try {
					self.writeCalllogWeb(callHistoryList);
				} catch (ExceptionManager e) {
					catchHelper("Error reading contacts", e, true);
				}
				if (self.doReporting) {
					try {
						self.writeCallLogToFile(callHistoryList);
					} catch (ExceptionManager e) {
						catchHelper("Error reading contacts", e, true);
					}
				}
			};
			workers.submitJob(callHistoryTask, 1);

			/* Create and write message history to file. */
			Runnable messageHistoryTask = () -> {
				try {
					MessageList = self.createMessageArray();
				} catch (SQLException | ExceptionManager e) {
					catchHelper("Error reading contacts", e, true);
				}
				try {
					self.writeMessagesWeb(MessageList);
				} catch (ExceptionManager e) {
					catchHelper("Error reading contacts", e, true);
				}
				if (self.doReporting) {
					try {
						self.writeMessageHistoryToFile(MessageList);
					} catch (ExceptionManager e) {
						catchHelper("Error reading contacts", e, true);
					}
				}
			};
			workers.submitJob(messageHistoryTask, 2);
			updateProgress(1.0f, 1.0f);

			/* Create and write the voice mail information to file. */
			Runnable vmailTask = () -> {
				try {
					VMailList = self.createVMailArray();
				} catch (NoSuchAlgorithmException | SQLException |
						ExceptionManager | IOException e) {
					catchHelper("Error reading contacts", e, true);
				}
				try {
					self.writeVMailWeb(VMailList);
				} catch (ExceptionManager | IOException e) {
					catchHelper("Error reading contacts", e, true);
				}
				if (self.doReporting ) {
					try {
						self.writeVMailToFile(VMailList);
					} catch (ExceptionManager e) {
						catchHelper("Error reading contacts", e, true);
					}
				}
			};
			workers.submitJob(vmailTask, 3);

			/*
			 * Write Safari history to file (if it exists - it was created
			 * in the encrypted section prior to now.
			 */
			if (iPhone.isEncrypted) {
				Runnable safariTask = () -> {
					try {
						SafariHistoryList = self.createSafariHistory();
					} catch (ExceptionManager e) {
						catchHelper("Error reading contacts", e, true);
					}
					try {
						self.writeSafariWeb(SafariHistoryList);
					} catch (ExceptionManager | IOException e) {
						catchHelper("Error reading contacts", e, true);
					}
					if (self.doReporting ) {
						try {
							self.writeBrowserToFile(SafariHistoryList);
						} catch (ExceptionManager e) {
							catchHelper("Error reading contacts", e, true);
						}
					}
				};
				workers.submitJob(safariTask, 4);
			}

			/* Wait about a minute for all shutdown. */
			workers.shutdown();
			workers.awaitTermination(30, TimeUnit.SECONDS);

		/* If reporting is set, do the reporting here. */
		if (self.doReporting) {
			Path reportFName = restoreLocation.resolve("PhoneReport.html");
			ReportManager phoneReport = new ReportManager(reportFName,
					iPhone);
			phoneReport.processMessages(MessageList);
		}

			updateMessage("Finishing up ...");
		} catch (Exception e) {
			throw new ExceptionManager("Critical Error decrypting files",
					e, true);
		}

	} catch (ExceptionManager | SQLException e) {
		/*
		 * Inform user there was an error somewhere.
		 * The ManagerException class will tell us if it was
		 * a fatal error and exit, or a basic error and
		 * continue.
		 */
		throw new ExceptionManager("Program encountered a critical error"
			+ " during initialization of the backup manager.", e, true);
	}
	/* Reached here without a fatal error? Extraction is done. */
	return null;
    }
        };
        PI.progressProperty().bind(backgroundTask.progressProperty());

        // Handle finish
        backgroundTask.setOnSucceeded(event -> {
            BMStage.close();
            onFinished.accept(true); /* Notify complete */
        });

        backgroundTask.setOnFailed(event -> {
            BMStage.close();
            onFinished.accept(false); /* Notify failure. */
        });

        Thread backgroundThread = new Thread(backgroundTask);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

/*
******************************************************************************
* These methods deal with decryption of the files in the backup.             *
******************************************************************************
*/
	    private void decryptDatabase() throws ExceptionManager, IOException,
	    	InvalidKeyException {
	        if (! this.iPhone.isEncrypted || this.iPhone.getKeyBag()
	        		.isEmpty()) {
	        	WindowManager.critical(ContextManager.getPrimaryStage(),
	        			"Fatal Error", "Critical error: The backup is "
	        			+ "encrypted but the keybag is empty");
	        }

	        byte[] manifestKey = new byte[this.iPhone.manifestKey.length() - 4];
	        ByteBuffer manifestKeyBuffer =
	        	ByteBuffer.wrap(this.iPhone.manifestKey.bytes());

	        int manifestClass =
	        	manifestKeyBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
	        manifestKeyBuffer.order(ByteOrder.BIG_ENDIAN).get(manifestKey);

	        /* Try to decrypt the Manifest.db file if encrypted. */
	        try {
	        	this.decryptedDatabaseFile = FilePairManager.MANIFEST
	        			.getDecryptedPath(this.backupLocation);
	            this.iPhone.getKeyBag().get().decryptFile(manifestClass,
	            	manifestKey, this.manifestDBFile,
	            	FilePairManager.MANIFEST
	            	.getDecryptedPath(this.restoreLocation));
	        } catch (FileNotFoundException e) {
	            throw new ExceptionManager("Error decrypting database file", e,
	            	true);
	        }
	    }

	    private boolean tryUnlock() throws ExceptionManager, IOException {
	        try {
	        	Optional<String> response = doRetPassword();
	        	String password = response.get();
	            iPhone.getKeyBag().get().unlock(password);
	            decryptDatabase();
	            return true;
	        } catch (InvalidKeyException e) {
	        	throw new ExceptionManager("Failure during unlock!", e,
	        			true);
	        }
	    }

/*
******************************************************************************
* These methods deal with the various media files, media directories, and    *
* extraction of media files.                                                 *
******************************************************************************
*/
	    /* XXX: Migrate to Window Manager, it will need MediaHeir - see methods below*/
    	public void createMediaPane() throws ExceptionManager {
    	    Dialog<String> dialog = new Dialog<>();
    	    dialog.setTitle("Image Directory List");
    	    dialog.setHeaderText("This backup has several image directories. "
    	        + "Please select a directory to extract or click the"
    	    	+ " \"Extract All\" button.");
    	    /* Lock background but do not initowner to avoid inheritance */
    	    dialog.initModality(Modality.APPLICATION_MODAL);
    	    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    	    dialog.getDialogPane().setPrefSize(600, 400);

    	    /* Progress spinner. It shows up on the pane which looks nice. */
    	    ProgressIndicator imgSpin = new ProgressIndicator();
    	    imgSpin.setVisible(false);

    	    /* TilePane for button placement. */
    	    TilePane content = new TilePane();
    	    content.setHgap(25);
    	    content.setVgap(25);
    	    content.setPadding(new Insets(20));

    	    /* Create an arraylist of buttons. Buttons!! :) */
    	    ArrayList<Button> buttonlist = new ArrayList<>();

    	    for (int i = 0; i < this.getMediaHier().size(); i++) {
    	        Path realMediaDir = this.getMediaHier().get(i).getFileName();

    	        /* Skip the DCIM directory entry */
    	        if (realMediaDir.toString().equals("DCIM")) {
    	            continue;
    	        }

    	        Button entry = new Button(realMediaDir.toString());
    	        entry.setOnAction(new EventHandler<ActionEvent>() {
    	            @Override
    	            public void handle(ActionEvent event) {
    	                imgSpin.setVisible(true);
    	                entry.setDisable(true);
    	                Task<Void> task = new Task<>() {
    	                    @Override
    	                    protected Void call() throws ExceptionManager {
    	                        copyMediaByDir(MediaList,
    	                        		realMediaDir.toString());
    	                        return null;
    	                    }
    	                };
    	                /* On success: disable the button, color it green. */
    	                task.setOnSucceeded(e -> {
    	                	imgSpin.setVisible(false);
    	                	entry.setDisable(true);
    	                	entry.setStyle("-fx-background-color: #4CAF50;");
    	                });
    	                task.setOnFailed(e -> {
    	                    imgSpin.setVisible(false);
    	                    entry.setDisable(false);
    	                    WindowManager.warning(ContextManager
    	                    	.getPrimaryStage(),
    	                        "Data Extraction Error",
    	                        "Error extracting files. Please review the"
    	                        + " extracted images.").show();
    	                });
    	                new Thread(task).start();
    	            }
    	        });
    	        buttonlist.add(entry);
    	    }

    	    /* Add an extract all button. */
    	    Button AllMedia = new Button("Extract All");
    	    AllMedia.setOnAction(new EventHandler<ActionEvent>() {
    	        @Override
    	        public void handle(ActionEvent event) {
    	            imgSpin.setVisible(true);
    	            AllMedia.setDisable(true);

    	            Task<Void> task = new Task<>() {
    	                @Override
    	                protected Void call() throws ExceptionManager {
    	                    copyAllMedia(MediaList);
    	                    return null;
    	                }
    	            };
    	            /* On success: disable the button, color it green. */
    	            task.setOnSucceeded(e -> {
    	            	imgSpin.setVisible(false);
    	            	AllMedia.setDisable(true);
    	            	AllMedia.setStyle("-fx-background-color: #4CAF50;");
    	            });
    	            task.setOnFailed(e -> {
    	                imgSpin.setVisible(false);
    	                AllMedia.setDisable(false);
    	                WindowManager.warning(ContextManager.getPrimaryStage(),
    	                    "Data Extraction Error",
    	                    "Error extracting files. Please review the"
    	                    + " extracted images.").show();
    	            });
    	            new Thread(task).start();
    	        }
    	    });

    	    buttonlist.add(AllMedia);
    	    content.getChildren().addAll(buttonlist);
    	    content.setAlignment(Pos.CENTER_LEFT);

    	    /* Put everything in a nice VBox for our end user. */
    	    VBox fullLayout = new VBox(20, content, imgSpin);
    	    fullLayout.setPadding(new Insets(15));
    	    fullLayout.setAlignment(Pos.CENTER);

    	    dialog.getDialogPane().setContent(fullLayout);
    	    dialog.showAndWait();
    	}

    	private Optional<String> doRetPassword() {
    	    final CompletableFuture<Optional<String>> result =
    	    		new CompletableFuture<>();

    	    Platform.runLater(() -> {
    	        Dialog<String> passWindow = new Dialog<>();
    	        passWindow.setHeaderText("This backup is encrypted and"
    	        		+ " needs a password.");
    	        passWindow.setTitle("Backup Password");
    	        passWindow.getDialogPane().getButtonTypes()
    	        	.addAll(ButtonType.OK, ButtonType.CANCEL);

    	        PasswordField passField = new PasswordField();

    	        HBox passData = new HBox();
    	        passData.setAlignment(Pos.CENTER_LEFT);
    	        passData.setSpacing(10);
    	        passData.getChildren().addAll(new
    	        		Label("Enter the password here:"), passField);
    	        passWindow.getDialogPane().setContent(passData);

    	        /* XXX: Simplify to if else */
    	        passWindow.setResultConverter(pressedButton ->
    	            pressedButton == ButtonType.OK ?
    	            		passField.getText() : null);
    	        result.complete(passWindow.showAndWait());
    	    });

    	    /* Block the background thread until the user responds. */
    	    try {
    	        return result.get();
    	    } catch (Exception e) {
    	    	WindowManager.warning(ContextManager.getPrimaryStage(),
    	    			"Error getting password!",
    	    			"No password was provided. A password must be provided"
    	    			+ " to decrypt an encrypted backup.");
    	        return Optional.empty();
    	    }
    	}

/*
******************************************************************************
* Create the contact list.                                                   *
******************************************************************************
*/
    private ArrayList<Contact> createContactArray() throws ExceptionManager,
    	SQLException {
    	ArrayList<Contact> cList = new ArrayList<>();

    	try (DatabaseFileManager dbMGR = new DatabaseFileManager(
    				FilePairManager.CONTACTS
    				.getDecryptedPath(this.restoreLocation));
    		Connection conn = dbMGR.getConnection();
    		PreparedStatement contactSTMT =
    				conn.prepareStatement(DatabaseQueriesManager
    				.getContactsQuery());
    		ResultSet contactres = contactSTMT.executeQuery()) {
    		/* This is long with many assignments. */
    		while (contactres.next()) {
    			String firstname = Optional.ofNullable(contactres
    					.getString("First")).orElse("Unknown First Name");
    			String lastname = Optional.ofNullable(contactres
    					.getString("Last")).orElse("Unknown Last Name");
    			String org = Optional.ofNullable(contactres
    					.getString("organization")).orElse("Unknown");
    			String dept = Optional.ofNullable(contactres
    					.getString("department")).orElse("Unknown");
    			String birthday = Optional.ofNullable(contactres
    					.getString("birthday")).orElse("Unknown");
    			String jobtitle = Optional.ofNullable(contactres
    					.getString("jobtitle")).orElse("Unknown");
    			String note = Optional.ofNullable(contactres
    					.getString("Note")).orElse("Unknown");
    			String nickname = Optional.ofNullable(contactres
    					.getString("Nickname")).orElse("Unknown");
    			String addate = Optional.ofNullable(contactres
    					.getString("Created")).orElse("Unknown");
    			String moddate = Optional.ofNullable(contactres
    					.getString("Modified")).orElse("Unknown");
    			String phone_work = Optional.ofNullable(contactres
    					.getString("phone_work")).orElse("Unknown");
    			String phone_mobile = Optional.ofNullable(contactres
    					.getString("phone_mobile")).orElse("Unknown");
    			String phone_home = Optional.ofNullable(contactres
    					.getString("phone_home")).orElse("Unknown");
    			String addy = Optional.ofNullable(contactres
    					.getString("address")).orElse("Unknown");
    			String city = Optional.ofNullable(contactres
    					.getString("city")).orElse("Unknown");
    			String email = Optional.ofNullable(contactres
    					.getString("email")).orElse("Unknown");
    			/* Now group them up into a single object. */
    			Contact temp = new Contact(firstname, lastname);
    			temp.setContactOrg(org);
    			temp.setContactDept(dept);
    			temp.setContactBirthday(birthday);
    			temp.setContactJobTitle(jobtitle);
    			temp.SetContactNote(note);
    			temp.setContactNickname(nickname);
    			temp.setContactAddDate(addate);
    			temp.setContactModDate(moddate);
    			temp.setContactPhone_Work(phone_work);
    			temp.setContactPhone_Mobile(phone_mobile);
    			temp.setContactPhone_Home(phone_home);
    			temp.setContactAddress(addy);
    			temp.setContactCity(city);
    			temp.setContactEmail(email);
    			/* Add this object to our list. */
    			cList.add(temp);
    		}
    		contactres.close();
    	} catch (Exception e) {
    		/* Use the ManagerException class even though this is SQL. */
    		throw new ExceptionManager("Unable to access the contacts"
    			+ " database, this is a fatal error and the"
    			+ " program will now exit.", e, true);
    	}
		return cList;
    }

/*
******************************************************************************
* Create the media items list.                                               *
******************************************************************************
*/
    private ArrayList<Media> createMediaArray() throws SQLException,
    	ExceptionManager {
    	/* Use the internal version of this file to avoid additional IO */
    	ArrayList<Media> mList = new ArrayList<>();
    	NSDictionary dataBlob = null;

    	try (DatabaseFileManager dbMGR = new DatabaseFileManager(this
    				.decryptedDatabaseFile);
    		Connection conn = dbMGR.getConnection();
			/* Now we have a database connection, build media list. */
			PreparedStatement stmt0 =
					conn.prepareStatement(DatabaseQueriesManager
					.getMediaFilesQuery());
			ResultSet results0 = stmt0.executeQuery()) {

    		/* If either are null, we have an issue (DB corruption?) */
			while (results0.next()) {
				/* Get data by column name. */
				String fileID = results0.getString("fileID");
				Path realFile = (this.backupLocation).resolve(fileID
						.substring(0,2)).resolve(fileID);
				String relativePath = results0.getString("relativePath");
				try {
					dataBlob = (NSDictionary)
						PropertyListParser.parse(results0.getBinaryStream(4));
				} catch (IOException | PropertyListFormatException |
					ParseException | ParserConfigurationException
					| SAXException e) {
					throw new ExceptionManager("Unable to get file properties"
							+ " for file decryption: ", e, true);
				}
				EncryptedFile mediaFile = new EncryptedFile(realFile,
						this.iPhone, dataBlob);
				/* Do not hash a directory (they do not exist on disk). */
				Media temp = new Media(fileID, relativePath,
					this.backupLocation, this.restoreLocation, mediaFile);
				mList.add(temp);
			}
			results0.close();
		} catch (Exception e) {
			/* Pass this exception to the ManagerException class. */
			throw new ExceptionManager("Encountered a database error while"
			    + " reading the database. This error is fatal and the"
			    + " program will now exit.", e, true);
		}
		return mList;
    }

/*
******************************************************************************
* Create the message history list.                                           *
******************************************************************************
*/
	private ArrayList<Message> createMessageArray() throws SQLException,
		ExceptionManager {

		ArrayList<Message> mList = new ArrayList<>();

		try (DatabaseFileManager dbMGR = new DatabaseFileManager(
				FilePairManager.MESSAGES
				.getDecryptedPath(this.restoreLocation));
				Connection conn = dbMGR.getConnection();
		    PreparedStatement msgstatement =
			conn.prepareStatement(DatabaseQueriesManager
					.getTextMessageQuery());
		    ResultSet msgResults = msgstatement.executeQuery()) {

		    while (msgResults.next()) {
		    	String ThreadId = Optional.ofNullable(msgResults
		    			.getString("ThreadId")).orElse("Unknown");
		    	int FromMe = msgResults.getInt("IsFromMe");
		    	String FromPhone = Optional.ofNullable(msgResults
		    			.getString("FromPhoneNumber")).orElse("Unknown");
		    	String ToPhone = Optional.ofNullable(msgResults
		    			.getString("ToPhoneNumber")).orElse("Unknown");
		    	String Service = Optional.ofNullable(msgResults
		    			.getString("Service")).orElse("Unknown");
		    	String txtDate = Optional.ofNullable(msgResults
		    			.getString("TextDate")).orElse("Unknown");
		    	String msgData = Optional.ofNullable(msgResults
		    			.getString("MessageText")).orElse("Unknown");

		    	msgData = msgData.replace("\r", "");
		    	msgData = msgData.replace("\n", "");
		    	Boolean IsFromMe = false;
		    	/* If it is from me, flip to true. */
		    	if (FromMe == 1) {
		    		IsFromMe = true;
		    	}
		    	Message tempmsg = new Message(ThreadId, IsFromMe, FromPhone,
		    			ToPhone, Service, txtDate, msgData);
		    	mList.add(tempmsg);
		    }
		    msgResults.close();
		} catch (Exception e) {
			throw new ExceptionManager("Failed to read SMS history"
					+ " from the backup files. This is a fatal error"
					+ " and the program will now exit.", e, true);
		}
		return mList;
    }

/*
******************************************************************************
* Create the Voice Mail items list.                                          *
******************************************************************************
*/
	private ArrayList<VoiceMail> createVMailArray() throws SQLException,
		ExceptionManager, NoSuchAlgorithmException, IOException {
		ArrayList<VoiceMail> vList = new ArrayList<>();

		/*
		 * The db file contains the vmail file info, the Manifest file
		 * contains the file ID, which we need to get for the copy.
		 */
		try (DatabaseFileManager vmdbMGR = new DatabaseFileManager(
					FilePairManager.VOICEMAILS.getDecryptedPath(
					this.restoreLocation));
				DatabaseFileManager dbMGR = new DatabaseFileManager(
					this.decryptedDatabaseFile);
				Connection vdbConn = vmdbMGR.getConnection();
				Connection manConn = dbMGR.getConnection();
				PreparedStatement vMailstmt =
						vdbConn.prepareStatement(DatabaseQueriesManager
						.getVMailDataQuery());
				ResultSet vMailData = vMailstmt.executeQuery()) {

			while (vMailData.next()) {
				NSDictionary dataBlob = null;
				/* ROWID contains the vmail file name. */
				String RowID = Optional.ofNullable(vMailData
						.getString("ROWID")).orElse("Unknown");
				String AppleDate = Optional.ofNullable(vMailData
						.getString("date")).orElse("Unknown");
				String Sender = Optional.ofNullable(vMailData
						.getString("sender")).orElse("Unknown");
				String Len = Optional.ofNullable(vMailData
						.getString("duration")).orElse("Unknown");
				String Expiration = Optional.ofNullable(vMailData
						.getString("expiration")).orElse("Unknown");
				String TrashDT = Optional.ofNullable(vMailData
						.getString("trashed_date")).orElse("Unknown");
				String Receiver = Optional.ofNullable(vMailData
						.getString("receiver")).orElse("Unknown");
				String Arrived = Optional.ofNullable(vMailData
						.getString("arrival_date")).orElse("Unknown");
				/* For the filename, change the ":" to "-" here. */
				String ArrivedTime = Arrived.replaceAll(":", "-");

				/* Get the file information (id/path) */
				/* ID should be changed to int? */
				try (PreparedStatement vFile =
					manConn.prepareStatement(DatabaseQueriesManager
							.getVMailFileQuery(RowID));
				ResultSet vFileData = vFile.executeQuery()) {
				String vfileID = vFileData.getString("fileID");
				
					dataBlob = (NSDictionary)
						PropertyListParser.parse(vFileData
						.getBinaryStream(2));

				/* Create a decent filename string. */
				String newFileName = Sender + "_" + ArrivedTime + ".amr";
				/* Copy the vmail file over to the extract directory. */
				Path dstFile = this.restoreLocation.resolve("VoiceMails")
						.resolve(newFileName);
				Path srcFile = this.backupLocation.resolve(vfileID
						.substring(0,2)).resolve(vfileID);


				/* Encrypted copy of files. */
				if (this.iPhone.isEncrypted) {
					EncryptedFile vmailFile = new 
						EncryptedFile(this.backupLocation
						.resolve(vfileID.substring(0, 2)).resolve(vfileID),
						this.iPhone, dataBlob);
					vmailFile.extract(dstFile);
				} else {
					/* Unencrypted extraction of files. */
					copyMediaByFile(srcFile, dstFile);
				}

				String MD5 = super.getFileHash("MD5", srcFile.toString());
				VoiceMail tmpVMail = new VoiceMail(RowID, AppleDate, Sender,
					Expiration, TrashDT, Receiver, Arrived, Len, MD5);
				vList.add(tmpVMail);
			
			} catch (IOException | PropertyListFormatException |
					ParseException | ParserConfigurationException
					| SAXException e) {
					throw new ExceptionManager("Error in voicemail extraction",
							e, false);
				}
			}
		} catch (Exception e) {
			throw new ExceptionManager("Error while retrieving voice mail"
				+ " data from the database. This is a fatal error and"
				+ " the program will now exit. The error was: ", e,
				true);
		}
		return vList;
	}

	private void writeVMailWeb(ArrayList<VoiceMail> vList) throws
		ExceptionManager, IOException {
		try (BufferedWriter webFile = Files.newBufferedWriter(FilePairManager
				.VOICEMAILSHTML.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {

			webFile.append(WebPageManager.header("Voicemail Data"));
			/* WebPageManager supports c-style varargs */
			webFile.append(WebPageManager.beginTable("Apple Date", "Sender",
					"Expiration", "Trashed date", "Receiver", "Received Date",
					"Duration (seconds)", "File MD5"));
			for (VoiceMail vm : vList) {
				webFile.append(WebPageManager.row(vm.getAppleDate(),
						vm.getSender(), vm.getExpiration(),
						vm.getTrashed_date(), vm.getReceiver(),
						vm.getArrival_date(), vm.getDuration(), vm.getMD5()));
			}
			webFile.append(WebPageManager.endTable());
			webFile.append(WebPageManager.footer());
		} catch (IOException e) {
			throw new ExceptionManager("Error writing voicemail report",
					e, false);
		}
	}

	private void writeVMailToFile(ArrayList<VoiceMail> vList)
		throws ExceptionManager {

			try (BufferedWriter outputFile = Files
					.newBufferedWriter(FilePairManager.VOICEMAILSCSV
					.getDecryptedPath(this.restoreLocation),
					StandardCharsets.UTF_8)) {

				outputFile.append(getVMailHeader());
				for (VoiceMail vm : vList) {
					outputFile.append(escapeCsv(vm
							.getAppleDate())).append(",");
					outputFile.append(escapeCsv(vm
							.getSender())).append(",");
					outputFile.append(escapeCsv(vm
							.getExpiration())).append(",");
					outputFile.append(escapeCsv(vm
							.getTrashed_date())).append(",");
					outputFile.append(escapeCsv(vm
							.getReceiver())).append(",");
					outputFile.append(escapeCsv(vm
							.getArrival_date())).append(",");
					outputFile.append(escapeCsv(vm
							.getDuration())).append(",");
					outputFile.append(vm.getMD5()).append(",");
					outputFile.append("\n");
				}
			} catch (IOException e) {
				throw new ExceptionManager("Unable to write Voice Mail"
						+ " information to file. This is not a fatal"
						+ " error and program use may continue.", e, false);
			}
	}

	private String getVMailHeader() {
		return "Apple Date,Sending Number,Expiration Date,Sent to Trash Date,"
				+ "Receiving Number,VoiceMail Arrival Date and Time,"
				+ "Duration in Seconds,MD5 Sum\n";
	}

/*
 *****************************************************************************
 * Create media hierarchy methods. These need to be publicly available.      *
 *****************************************************************************
 */

    private ArrayList<Path> getMediaHier() throws ExceptionManager {
		ArrayList<Path> dList = new ArrayList<>();

		/* Build an SQL statement for SQLite. */
		String sqlStatement = DatabaseQueriesManager.getDirectoryQuery();
		try (DatabaseFileManager dbMGR = new 
				DatabaseFileManager(FilePairManager.MANIFEST
					.getDecryptedPath(this.restoreLocation));
			Connection Conn = dbMGR.getConnection();
			PreparedStatement statement = Conn.prepareStatement(sqlStatement);
			ResultSet res = statement.executeQuery()) {

			while (res.next()) {
				String relPath = res.getString("relativePath");
				dList.add(Paths.get(relPath));
			}
			/* Close this result resources, others are closed elsewhere. */
		} catch (Exception e) {
			throw new ExceptionManager("Failed reading directory hierarchy"
				+ " from the database. This is a fatal error and the"
				+ " program will now exit.", e, true);
		}
		return dList;
	}

    /*
     * Attempt to build a media hierarchy in the file system.
     */
	private int createMediaHier() throws ExceptionManager {

		ArrayList<Path> DirList = getMediaHier();
		Path fileDir, vmailDir;

		/* Create the directory hierarchy based on database entries. */
		for (int i = 0; i < DirList.size(); i++) {
			fileDir = this.restoreLocation.resolve(DirList.get(i));

			try {
				/* Not creating directories is a fatal error. */
				Files.createDirectories(fileDir);
			} catch (IOException e) {
				throw new ExceptionManager("Fatal error attempting to"
					+ " create directory hierarchy. This is a fatal"
					+ " error and the program will now exit.",
					e, true);
			}
		}
		/* Now create the Voice Mail directory */
		try {
			vmailDir = this.restoreLocation.resolve("VoiceMails");
			Files.createDirectories(vmailDir);
		} catch (IOException e) {
			throw new ExceptionManager("Fata error attempting to create"
				+ " the voicemail extraction directory. This is a"
				+ " fatal error and the program will now exit.",
				e, true);
		}
		return 0;
	}

/*
******************************************************************************
* Safari browsing history management methods.                                *
******************************************************************************
*/
	private ArrayList<SafariHistory> createSafariHistory()
		throws ExceptionManager {
		ArrayList<SafariHistory> browserHistory =
			new ArrayList<>();

		try (DatabaseFileManager dbMGR = new 
				DatabaseFileManager(FilePairManager.SAFARI
						.getDecryptedPath(this.restoreLocation));
				Connection Conn = dbMGR.getConnection();
				PreparedStatement histSTMT =
						Conn.prepareStatement(DatabaseQueriesManager
						.getSafariHistoryQuery());
				ResultSet historyData = histSTMT.executeQuery()) {

			while (historyData.next()) {
				String visitDateTime = Optional.ofNullable(historyData
						.getString("visit_times")).orElse("Unknown");
				String siteTitle = Optional.ofNullable(historyData
						.getString("site_title")).orElse("Unknown");
				String url = Optional.ofNullable(historyData
						.getString("site_url")).orElse("Unknown");
				String visitCount = Optional
						.ofNullable(historyData.getString("visit_count"))
						.orElse("Unknown");
				SafariHistory browserEntry = new SafariHistory(
					visitDateTime, siteTitle, url, visitCount);
				browserHistory.add(browserEntry);
			}
		} catch (Exception e) {
			throw new ExceptionManager("Error reading Safari history."
				+ " This is a non-fatal error, we will try to continue.",
				e, false);
		}
		return browserHistory;
	}

	private void writeSafariWeb(ArrayList<SafariHistory> sfArr) throws
		ExceptionManager, IOException {

		try (BufferedWriter safariHistory = Files
				.newBufferedWriter(FilePairManager.SAFARIHTML
				.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {

			safariHistory.append(WebPageManager.header("Safari Browsing"
					+ "Data"));
			safariHistory.append(WebPageManager.beginTable("Site Visit Count",
					"Site Visit Date/Time", "Site Name", "Site URL"));
			for (SafariHistory historyItem : sfArr) {
				safariHistory.append(WebPageManager.row(historyItem
						.getVisitCount(), historyItem.getVisitTime(),
						historyItem.getSiteName(), historyItem.getSiteURL()));
			}
			safariHistory.append(WebPageManager.endTable());
			safariHistory.append(WebPageManager.footer());
		} catch (IOException e) {
			WindowManager.warning(ContextManager.getPrimaryStage(),
					"Non-fatal Error",
					"There was an error attempting to write the Safari HTML"
					+ " file.");
			throw new ExceptionManager("Error opening Safari HTML file",
					e, false);
		}
	}

	private void writeBrowserToFile(ArrayList<SafariHistory> historyArr)
		throws ExceptionManager {

		String fileHeader = "Visit Count,Visit Date and Time,"
				+ "Site Name,Site URL\n";

		try (BufferedWriter historyFile = Files
				.newBufferedWriter(FilePairManager.SAFARICSV
						.getDecryptedPath(this.restoreLocation),
						StandardCharsets.UTF_8)) {

			historyFile.append(fileHeader);
			for (SafariHistory historyItem : historyArr) {
				historyFile.append(escapeCsv(historyItem.getVisitCount()))
					.append(",");
				historyFile.append(escapeCsv(historyItem.getVisitTime()))
					.append(",");
				historyFile.append(escapeCsv(historyItem.getSiteName()))
					.append(",");
				historyFile.append(escapeCsv(historyItem.getSiteURL()))
					.append("\n");
			}

		} catch (IOException e) {
			throw new ExceptionManager("Unable to write Safari history data"
				+ " to file. This is a non-fatal error and the program"
				+ " will attempt to continue", e, false);
		}
	}

/*
******************************************************************************
* Media extraction methods.                                                  *
******************************************************************************
*/
	/* Write metadata for each file. Future need here. */
	/* XXX: I do not think metadata is useful to restore? Maybe ... */
	public void copyFileMetadata(Path localSRC, Path localDST) throws
	    IllegalAccessException {
		return;
	}

	/* Copy encrypted media by file, used on encrypted backups. */
	private void copyEncryptedMediaByFile(Media mediaFile, Path localDST) {
		Path destination = localDST;
		try {
			mediaFile.getMediaFile().extract(destination);
		} catch (ExceptionManager e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* Copy a single media file to our extraction directory. */
	private void copyMediaByFile(Path localSRC, Path localDST)
		throws ExceptionManager {

		/* Make sure the hash is a real file instead of just a hash entry. */
		if (! Files.exists(localSRC)) {
			return;
		}

		try {
			Files.copy(localSRC, localDST);
		} catch (FileAlreadyExistsException e) {
			throw new ExceptionManager("The file already exists and will"
					+ " not be overwritten! System said: ", e, false);
			/*
			 * XXX: With file already exists (probably because a single
			 * directory was extracted and now we are trying to extract
			 * again or extract all, an actual OK/Cancel box should be
			 * thrown here and a cancel call should be pushed back to
			 * the main application.
			 */
		} catch (IOException e) {
			/* This is a fatal error, we will exit here. */
			throw new ExceptionManager("Fatal IO error attempting to copy"
				+ " " + localSRC + " to " + localDST + ". This is"
				+ " a fatal error and the program will now exit.", e,
				true);
		} 
		return;
	}

	/* Copy an entire media directory to the extraction directory. */
	private void copyMediaByDir(ArrayList<Media> MediaList, String dir)
		throws ExceptionManager {
		int mediaTotal = MediaList.size();

		for (int i = 0; i < mediaTotal; i++) {
			/* Compare the directory request, extract only those files. */
			if (dir.equals(MediaList.get(i).getFileRealParent())) {
				/* Error checking is done in copyMediaByFile. */
				if (this.iPhone.isEncrypted) {
					copyEncryptedMediaByFile(MediaList.get(i),
						MediaList.get(i).getToLocation());
				} else {
					copyMediaByFile(MediaList.get(i).getFromLocation(),
						MediaList.get(i).getToLocation());
				}
			}
		}
	}

	/* Extract all media files (pictures/video) */
	public void copyAllMedia(ArrayList<Media> MediaList) throws
		ExceptionManager {
		int mediaTotal = MediaList.size();

		/* Error checking is done in copyMediaByFile. */
		for (int i = 0; i < mediaTotal; i++) {
			if (iPhone.isEncrypted) {
				copyEncryptedMediaByFile(MediaList.get(i),
					MediaList.get(i).getToLocation());
			} else {
				copyMediaByFile(MediaList.get(i).getFromLocation(),
					MediaList.get(i).getToLocation());
			}
		}
	}

/*
******************************************************************************
* Manage contact methods.                                                    *
******************************************************************************
*/
	private void writeContactsWeb(ArrayList<Contact> ContactList) throws
		ExceptionManager, IOException {
		try (BufferedWriter contactFile = Files
				.newBufferedWriter(FilePairManager.CONTACTSHTML
				.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {

			contactFile.append(WebPageManager.header("Stored Contact List"));
			contactFile.append(WebPageManager.beginTable("First Name", 
					"Last Name", "Organization", "Department", "Birthday",
					"Job Title", "Note", "Nick name", "Contact Add Date",
					"Contact modification Date", "Contact Work Phone",
					"Contact Mobile Phone", "Contact Home Phone",
					"Contact Address", "Contact City",
					"Contact Email Address"));
			for (Contact contact : ContactList) {
				contactFile.append(WebPageManager.row(contact.getContactFname(),
						contact.getContactLname(), contact.getContactOrg(),
						contact.getContactDept(), contact.getContactBirthday(),
						contact.getContactJobTitle(), contact.getContactNote(),
						contact.getContactNickname(),
						contact.getContactAddDate(),
						contact.getContactModDate(),
						contact.getContactPhoneWork(),
						contact.getContactPhoneMobile(),
						contact.getContactPhoneHome(),
						contact.getContactAddress(),
						contact.getContactCity(),
						contact.getContactEmail()));
			}
			contactFile.append(WebPageManager.endTable());
			contactFile.append(WebPageManager.footer());
		} catch (IOException e) {
			WindowManager.warning(ContextManager.getPrimaryStage(),
					"Error writing contacts data", "Error writing the"
					+ " contacts web report data file.");
			throw new ExceptionManager("Error writing contacts web file: ",
					e, false);
		}
		return;
	}

	/* Since we use the header in two places, provide a get method. */
	private String getContactsHeader() {
			return "First Name,Last Name,Organization,Department,Birthday,"
				+ "JobTitle,Note,Nickname,Creation Date,Modification"
				+ " Date,Work Phone,Mobile Phone,Home Phone,Address,"
				+ "City,Email\n";
	}

	/* Write a header, then write contacts to CSV file. */
	private void writeContactsToFile(ArrayList<Contact> ContactList)
		throws ExceptionManager {

		try (BufferedWriter outputFile = Files
				.newBufferedWriter(FilePairManager.CONTACTSCSV
				.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {

			outputFile.write(getContactsHeader());
			for (Contact contact : ContactList) {
				outputFile.append(escapeCsv(contact.getContactFname()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactLname()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactOrg()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactDept()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactBirthday()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactJobTitle()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactNote()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactNickname()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactAddDate()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactModDate()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactPhoneWork()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactPhoneMobile()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactPhoneHome()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactAddress()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactCity()))
					.append(",");
				outputFile.append(escapeCsv(contact.getContactEmail()))
					.append(",").append("\n");
			}
		} catch (IOException e) {
			/*
			 * Failure to write contacts is not fatal
			 * since we display them.
			 */
			throw new ExceptionManager("Fatal IO exception while attempting"
				+ " to write contacts to a file. This is a fatal error"
				+ " and the program will now exit.", e, true);
		}
		return;
	}

/*
******************************************************************************
* Text message history management methods.                                   *
******************************************************************************
*/
	/* Header for CSV message history file. */
	private String getMessagesHeader() {
		return "ThreadID,SentByMe,FromPhoneNumber,ToPhoneNumber,ServiceType,"
			+ "MessageDate,MessageData\n";
	}

	/* Write text message history to a CSV file. */
	private void writeMessageHistoryToFile(List<Message> messageList)
		throws ExceptionManager {

		try (BufferedWriter outputFile = Files
				.newBufferedWriter(FilePairManager.MESSAGESCSV
				.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {

			/* Drop the header into the file, then add looped data. */
			outputFile.write(getMessagesHeader());
			for (Message msg : MessageList) {
				outputFile.append(msg.getMessageThreadID()).append(",");
				/* Document message origin. */
				if (msg.getMessageIsFromMe()) {
					outputFile.append("Yes,");
				} else {
					outputFile.append("No,");
				}
				outputFile.append(escapeCsv(msg.getMessageFromPhone()))
					.append(",");
				outputFile.append(escapeCsv(msg.getMessageToPhone()))
					.append(",");
				outputFile.append(escapeCsv(msg.getMessageService()))
					.append(",");
				outputFile.append(escapeCsv(msg.getMessageDate()))
					.append(",");
				outputFile.append(escapeCsv(msg.getMessageData()))
					.append("\n");
			}
		} catch (IOException e) {
			throw new ExceptionManager("Unable to write text message history"
				+ " to file. This is a fatal error and the program"
				+ " will now exit.", e, true);
		}
	}

	private void writeMessagesWeb(ArrayList<Message> msgList) throws
		ExceptionManager {

		try (BufferedWriter msgFile = Files.newBufferedWriter(FilePairManager
				.MESSAGESHTML.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {
			msgFile.append(WebPageManager.header("Message History"));
			msgFile.append(WebPageManager.beginTable("Message Thread ID",
					"Message Originates from Me", "Message Source",
					"Message Destination", "Message Service Utilizied",
					"Message Send/Rec Date", "Message Contents"));
			for (Message msg : msgList) {
				msgFile.append(WebPageManager.row(msg.getMessageThreadID(),
						msg.getMessageIsFromMe().toString(),
						msg.getMessageFromPhone(),
						msg.getMessageToPhone(), msg.getMessageService(),
						msg.getMessageDate(), msg.getMessageData()));
			}
			msgFile.append(WebPageManager.endTable());
			msgFile.append(WebPageManager.footer());
		} catch (IOException e) {
			WindowManager.warning(ContextManager.getPrimaryStage(),
					"Error writing messages",
					"Error occured while writing message html report");
			throw new ExceptionManager("Error writing message html report: ",
					e, false);
		}
	}

/*
******************************************************************************
* Methods to deal with call history, including FaceTime.                     *
******************************************************************************
*/
	private ArrayList<PhoneCall> createCallHistory() throws ExceptionManager {

		ArrayList<PhoneCall> callHistory = new ArrayList<>();

		try (DatabaseFileManager dbMGR = new
				DatabaseFileManager(FilePairManager.CALLS
						.getDecryptedPath(this.restoreLocation));
				Connection Conn = dbMGR.getConnection();
				PreparedStatement callStatement =
						Conn.prepareStatement(DatabaseQueriesManager
						.getCallHistoryQuery());
				ResultSet callRes = callStatement.executeQuery()) {

			while (callRes.next()) {
				String dateTime = Optional.ofNullable(callRes
						.getString("calltime")).orElse("Unknown");
				String duration = Optional.ofNullable(callRes
						.getString("DURATION")).orElse("Unknown");
				String provider = Optional.ofNullable(callRes
						.getString("ZSERVICE_PROVIDER")).orElse("Unknown");
				String callNum = Optional.ofNullable(callRes
						.getString("number")).orElse("Unknown");

				int fromMe = callRes.getInt("ZORIGINATED");
				String whoCalledWho;
				if (fromMe == 0) {
					whoCalledWho = "To me";
				} else {
					whoCalledWho = "From me";
				}
				PhoneCall theCall =
					new PhoneCall(dateTime, duration, provider, callNum,
					whoCalledWho);
				callHistory.add(theCall);
			}
		} catch (Exception e) {
			throw new ExceptionManager("Fatal error reading call history,"
					+ " and the program must exit. The error was: ", e, true);
		}
		return callHistory;
	}

	private void writeCalllogWeb(ArrayList<PhoneCall> callHistory) throws
		ExceptionManager {

		try (BufferedWriter callLog = Files.newBufferedWriter(FilePairManager
				.CALLSHTML.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {
			callLog.append(WebPageManager.header("Call History"));
			callLog.append(WebPageManager.beginTable("Call Date/Time",
					"Call length (Seconds)", "Service Utilized", "Number",
					"Call Origin"));
			for (PhoneCall call : callHistory) {
				callLog.append(WebPageManager.row(call.getDateTime(),
						call.getCallLen(), call.getCallService(),
						call.getCallNumber(), call.getCallOrigin()));
			}
			callLog.append(WebPageManager.endTable());
			callLog.append(WebPageManager.footer());
		} catch (IOException e) {
			WindowManager.warning(ContextManager.getPrimaryStage(),
					"Error writing call log", "Unable to write call log to"
							+ " disk.").show();
			throw new ExceptionManager("Error writing call log: ",
					e, false);
		}
	}

	private void writeCallLogToFile(ArrayList<PhoneCall> callHistory)
		throws ExceptionManager {

		try (BufferedWriter callLog = Files.newBufferedWriter(FilePairManager
				.CALLSCSV.getDecryptedPath(this.restoreLocation),
				StandardCharsets.UTF_8)) {

	        callLog.append(getCallLogHeader());
	        
	        for (PhoneCall call : callHistory) {
	            callLog.append(escapeCsv(call.getDateTime())).append(",");
	            callLog.append(escapeCsv(call.getCallLen())).append(",");
	            callLog.append(escapeCsv(call.getCallService())).append(",");
	            callLog.append(escapeCsv(call.getCallNumber())).append(",");
	            callLog.append(escapeCsv(call.getCallOrigin())).append("\n");
	        }
		} catch (IOException e) {
			throw new ExceptionManager("Failed to write call history to"
				+ " a file. This is a fatal error, sorry, but the"
				+ " program must exit.", e, true);
		}
	}

	private String getCallLogHeader() {
		return "Date and Time, Duration, Service, Number, From\n";
	}

/*
 * This is for CSV file cleanup.
 */
	private String escapeCsv(String input) {
	    if (input == null) return "";
	    if (input.contains(",") || input.contains("\"") || 
	    		input.contains("\n")) {
	        return "\"" + input.replace("\"", "\"\"") + "\"";
	    }
	    return input;
	}

	/* Copy unencrypted files (used in unecrypted backups. */
	private void copyUnecryptedFiles() throws ExceptionManager {
		/*
		 * As far as I have been able to ascertain, Safari history is only
		 * backed up when the encryption option is selected. As such, I
		 * have not included it here. This may (or may have) changed since
		 * I last tested so I reserve the right to be wrong.
		 */
		try {
				Files.copy(FilePairManager.CALLS
						.getEncryptedPath(this.backupLocation),
						FilePairManager.CALLS
						.getDecryptedPath(this.restoreLocation));
				Files.copy(FilePairManager.CONTACTS
						.getEncryptedPath(this.backupLocation),
						FilePairManager.CONTACTS
						.getDecryptedPath(this.restoreLocation));
				Files.copy(FilePairManager.MANIFEST
						.getEncryptedPath(this.backupLocation),
						FilePairManager.MANIFEST
						.getDecryptedPath(this.restoreLocation));
				Files.copy(FilePairManager.MESSAGES
						.getEncryptedPath(this.backupLocation),
						FilePairManager.MESSAGES
						.getDecryptedPath(this.restoreLocation));
				Files.copy(FilePairManager.VOICEMAILS
						.getEncryptedPath(this.backupLocation),
						FilePairManager.VOICEMAILS
						.getDecryptedPath(this.restoreLocation));
		} catch (IOException e) {
				throw new ExceptionManager("Unable to perform copy of"
					+ " critical files: ", e, true);
		}
		return;
	}

/*
****************************************************************************
* Method to return important information about this backup.                *
****************************************************************************
*/
	/* Is the backup encrypted? */
	public Boolean getIsEncrypted() {
		return this.isEncrypted;
	}

	private void catchHelper(String reason, Exception e, boolean fatal)
			throws ExceptionManager {
		throw new ExceptionManager(reason, e, fatal);
	}
}
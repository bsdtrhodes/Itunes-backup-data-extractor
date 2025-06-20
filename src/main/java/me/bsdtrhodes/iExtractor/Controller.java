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

/*
 * My first attempt at a JavaFX application with Scene Builder and I am
 * sure there are mistakes or better ways to do things. Important notes:
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Set;

import javafx.application.Platform;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

	/*
	 * The controller provides various methods within
	 * the GUI.
	 */
public class Controller {
	/*
	 * My understanding is that the controller initialization is also
	 * an instance of a controller and as such, variables here are
	 * set with "this." If this turns out to be wrong, it will be
	 * fixed.
	 */
	private Path backupLocation, restoreLocation;

	/* We set this if the user would like a final report of the extraction. */
	private boolean doReporting = false;

	/* Ensure that the work was complete before showing data. */
	private boolean isComplete = false;

    /* We instantiate this here to prevent warnings/null references. */
	private BackupManager NewBackup = null;


	/* JavaFX initializers that are needed to run the application. */
	public Controller() {
	}

	void initialize() {
	}

/*
******************************************************************************
* The below area is the GUI FXML button/menu tree.                           *
******************************************************************************
*/
    @FXML
    private MenuItem BeginExport;

    @FXML
    private Menu FileMenu;

    @FXML
    private Menu HelpMenu;

    @FXML
    private Menu MenuAbout;

    @FXML
    private AnchorPane MainAnchorPane;

    @FXML
    private MenuBar MenuBar;

    @FXML
    private MenuItem MenuQuit;

    @FXML
    private MenuItem HashDump;

    @FXML
    private MenuItem setReporting;

    @FXML
    private VBox VBox;

    @FXML
    private Button contactsBTN;

    @FXML
    private Button callHistoryBTN;

    @FXML
    private Button mediaBTN;

    @FXML
    private Button messagesBTN;

    @FXML
    private Button reportBTN;

    @FXML
    private MenuItem setBackupLocation;

    @FXML
    private MenuItem setExportLocation;

    @FXML
    private Button voicemailBTN;

    @FXML
    private Button webBTN;

/*
******************************************************************************
* The below area is where we handle actions for buttons/menus.               *
******************************************************************************
*/
    @FXML
    public void doShowAbout(ActionEvent event) throws ExceptionManager {
    	WindowManager.doShowAbout();
    }

    @FXML
    public void doShowHelp(ActionEvent event) throws ExceptionManager {
    	WindowManager.doShowHelp();
    }

    @FXML
    public void doCreateReport(ActionEvent event) {
    	this.doReporting = true;
    	if (this.doReporting) {
    		setReporting.setText("✔ Create Report");
    	}
    }

    @FXML
    public void doShowReport(ActionEvent event) throws ExceptionManager {
    	/* 
    	 * Two qualifiers must be met before showing a report. First,
    	 * the extraction had to have completed successfully. Second,
    	 * the report option must be selected prior to running an
    	 * extraction. Test for both or one of the two and react
    	 * accordingly.
    	 */
    	if ((this.doReporting) && (this.isComplete)) {
    		WindowManager.doShowReport(this.restoreLocation);
    	} else if ((this.isComplete) && (! this.doReporting)) {
    		String message = "No report was generated during extraction."
    				+ " To generate a report, please set it prior to running"
    				+ " an extraction.";
    		WindowManager.generic(ContextManager.getPrimaryStage(),
    				"Report option not set", message).show();
    		return;
    	} else {
    		String message = "Unable to display a report until an"
    				+ " extraction has been run with the report option"
    				+ " selected in the File menu.";
    		WindowManager.generic(ContextManager.getPrimaryStage(),
    				"Notice", message).show();
    		return;
    	}
    }

    @FXML
    public void doExtractContacts(ActionEvent event) throws ExceptionManager {
    	if (! this.isComplete) {
			doProgramFlow();
    	} else {
    		WindowManager.doShowContacts(this.restoreLocation);
    	}
    }

    @FXML
    public void doHashDump(ActionEvent event) throws ExceptionManager {
    	this.backupLocation = retBackupLocation();
    	if (this.backupLocation == null) {
    		WindowManager.generic(ContextManager.getPrimaryStage(),
    				"No backup directory selected!", "Cannot provide hashes"
    				+ " until a backup directory has been selected.");
    		return;
    	}
		Key2HashCat hashKey = new Key2HashCat(this.backupLocation);
    	Set <String> hashSet = hashKey.getHash();

    	/* Text box for all found hashes data. */
    	StringBuilder hashes = new StringBuilder();
    	for (String hash : hashSet) {
    		hashes.append(hash).append("\n");
    	}
    	WindowManager.generic(ContextManager.getPrimaryStage(),
    			"All found hashes in this backup", hashes.toString()).show();
    }

    @FXML
    public void doExtractMedia(ActionEvent event) throws ExceptionManager {
		/* If a backup extract was not run, issue a warning to user. */
		if (! this.isComplete) {
			doProgramFlow();
		} else {
			NewBackup.createMediaPane();
		}
    }

    @FXML
    public void doExtractMessages(ActionEvent event) throws ExceptionManager {
		if (! this.isComplete) {
			/* Text box for clicks before running an extraction. */
			doProgramFlow();
			return;
		} else {
			WindowManager.doShowMessageHistory(this.restoreLocation);
		}
    }

    @FXML
    public void doExtractVoicemail(ActionEvent event) throws ExceptionManager {
		if (! this.isComplete) {
			doProgramFlow();
		} else {
			WindowManager.doShowVMList(this.restoreLocation);
		}
    }

    @FXML
    public void doExtractWeb(ActionEvent event) throws ExceptionManager {
		if (! this.isComplete) {
			/* Text box for clicks before running an extraction. */
			String message = "Cannot offer Safari browsing"
					+ " history until an extraction was ran from the"
					+ " drop down menu.";
			WindowManager.generic(ContextManager.getPrimaryStage(), "Notice",
					message).show();
			return;
		} else if (! NewBackup.phoneIsEncrypted) {
			String message = "Could not locate the Safari browsing"
					+ " history in this backup. This information is only"
					+ " available when an encrypted backup was taken.";
			WindowManager.generic(ContextManager.getPrimaryStage(), "Notice",
					message).show();
			return;
		} else {
			WindowManager.doShowSafariHistory(this.restoreLocation);
		}
    }

    @FXML
    public void doShowCallHistory(ActionEvent event) throws ExceptionManager {
    	if (! this.isComplete) {
    		String message = "Cannot offer call history until"
    				+ " an extraction has been run from the File menu.";
    		WindowManager.generic(ContextManager.getPrimaryStage(), "Notice",
    				message).show();
    		return;
    	} else {
    		WindowManager.doShowCallHistory(this.restoreLocation);
    	}
    }

	@FXML
	public void MenuQuitOption(ActionEvent event) {
		Platform.exit();
	}

	@FXML
	public void doSetBackupFile(ActionEvent event) {
		this.backupLocation = retBackupLocation();
		if (this.backupLocation != null) {
			setBackupLocation.setText("✔ Backup Location Set");
		}
	}

    @FXML
    public void doSetExtractDir(ActionEvent event) {
    	this.restoreLocation = retExtractLocation();
    	if (this.backupLocation != null) {
    		setExportLocation.setText("✔ Restore Location Set");
    	}
    }

    @FXML
    public void doExtraction(ActionEvent event) throws ExceptionManager {
    	if ((this.backupLocation == null) || (this.restoreLocation == null)) {
    		/* The backup file is empty, warn user. */
    		String message = "One location has not been defined,"
    				+ " please select both a backup and restore location.";
    		WindowManager.warning(ContextManager.getPrimaryStage(),
    				"Warning", message).show();
    		return;
    	}
    	doProgramFlow();
    }

/*
******************************************************************************
* These methods do the work after various button clicks/menu choices         *
******************************************************************************
*/
	/* Methods to GET directory locations. */
    /* 
     * Set the default location to UserHome, if you use the MS AppStore the
     * location is: UserHome\Apple\MobileSync\backup, otherwise
     * it is in UserHome\Roaming\Apple Computer\MobileSync\backup.
     */
    private Path retBackupLocation() {
        String userHome = System.getProperty("user.home");
        File startDir = new File(userHome);

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select an iTunes Backup Directory");
        chooser.setInitialDirectory(startDir);

        File selectedDir = chooser.showDialog(null);
        if (selectedDir == null) {
            return null;
        } else {
            return selectedDir.toPath();
        }
    }

	private Path retExtractLocation() {
	    String userHome = System.getProperty("user.home");
	    File startDir = new File(userHome);

	    DirectoryChooser chooser = new DirectoryChooser();
	    chooser.setTitle("Select an iTunes Backup Directory");
	    chooser.setInitialDirectory(startDir);

	    File selectedDir = chooser.showDialog(null);
	    if (selectedDir == null) {
	        return null;
	    } else {
	        return selectedDir.toPath();
	    }
	}

	private void doProgramFlow() throws ExceptionManager {
	    if (this.backupLocation == null) {
	        this.backupLocation = retBackupLocation();
	    }

	    if (this.restoreLocation == null) {
	        this.restoreLocation = retExtractLocation();
	    }

	    if (this.backupLocation == null || this.restoreLocation == null) {
	        WindowManager.generic(
	            ContextManager.getPrimaryStage(),
	            "Not all options selected",
	            "Please set both backup and restore locations prior to"
	            + " execution.").show();
	        return;
	    }

	    if (! this.isComplete && WindowManager.askYesNoBegin()) {
	        try {
	            BackupManager NewBackup = new BackupManager(
	                this.backupLocation,
	                this.restoreLocation,
	                this.doReporting
	            );
	            this.NewBackup = NewBackup;

	            NewBackup.doAllWorkAsync(success -> {
	                if (success) {
	                    this.isComplete = true;
	                } else {
	                    WindowManager.critical(
	                        ContextManager.getPrimaryStage(),
	                        "Generic failure",
	                        "The extractor was unable to complete the"
	                        + " backup.").show();
	                }
	            });

	        } catch (ExceptionManager | SQLException | InvalidKeyException |
	                 NoSuchAlgorithmException | IOException e) {
	            WindowManager.showExceptionDialog(ContextManager
	            		.getPrimaryStage(), e, true);
	            throw new ExceptionManager(
	                "Failed to perform the required work to extract files. "
	                + "This is a fatal error and the program will now exit.",
	                e, true
	            );
	        }
	    }
	}

}
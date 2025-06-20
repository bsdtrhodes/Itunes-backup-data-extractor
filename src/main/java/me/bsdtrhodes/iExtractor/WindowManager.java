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

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * This is my class for building various error windows.
 * Notice windows are to inform the user.
 * Warning windows are for non-critical but important errors.
 * Critical windows are for non-exception critical errors where exit is called.
 * Exception dialog takes exceptions and calls exit based on severity.
 * 
 * If you are looking for logs, the ExceptionManager logs caught exceptions.
 * 
 * FYI:
 * This class no longer exposes internal Stage references directly.
 * Use ContextManager.getPrimaryStage() to access the app's main stage.
 */
public class WindowManager {
    private final Stage window;

    /* This constructor is for system messages. */
    private WindowManager(Stage owner, String title, String message,
    		boolean fatal) {
        this.window = new Stage();
        if (owner != null) {
            this.window.initOwner(owner);
            this.window.initModality(Modality.APPLICATION_MODAL);
        }

        /* Set up a text area for the message. */
        TextArea area = new TextArea(message);
        area.setWrapText(true);

        VBox theBox = new VBox(new Label(title), area);
        VBox.setVgrow(area, Priority.ALWAYS);

        Button clsButton = new Button("CLOSE");
        clsButton.setOnAction(e -> window.close());

        theBox.getChildren().add(clsButton);
        theBox.setSpacing(15);
        theBox.setPadding(new Insets(25));
        theBox.setAlignment(Pos.CENTER);

        window.setScene(new Scene(theBox));

        /* Exit if we are built with a fatal request. */
        if (fatal) {
            window.setOnHidden(e -> Platform.exit());
        }
    }

    /* This constructor takes nodes for data displays. */
    private WindowManager(Stage owner, Label title, Node content) {
        this.window = new Stage();

        if (owner != null) {
            this.window.initOwner(owner);
            this.window.initModality(Modality.APPLICATION_MODAL);
        }

        VBox theBox = new VBox(title, content);
        VBox.setVgrow(content, Priority.ALWAYS);

        Button clsButton = new Button("CLOSE");
        clsButton.setOnAction(e -> window.close());

        theBox.getChildren().add(clsButton);
        theBox.setSpacing(15);
        theBox.setPadding(new Insets(25));
        theBox.setAlignment(Pos.CENTER);

        this.window.setScene(new Scene(theBox));
    }

    /* Show the window we built. */
    public void show() {
        this.window.show();
    }

    /* Very generic message or information. */
    public static WindowManager generic(Stage owner, String title, String
    		message) {
        return new WindowManager(owner, title, message, false);
    }

    /* Show a warning window, used for important messages. */
    public static WindowManager warning(Stage owner, String title, String
    		message) {
        return new WindowManager(owner, "⚠ " + title, message, false);
    }

    /*  Show a critical window for anything that is fatal. This calls exit. */
    public static WindowManager critical(Stage owner, String title, String
    		message) {
        return new WindowManager(owner, "✖ " + title, message, true);
    }

    public static void showExceptionDialog(Stage owner, Exception ex,
    		boolean fatal) {
        String error = ex.getMessage();

        Platform.runLater(() -> {
            WindowManager wm;
            if (fatal) {
                wm = WindowManager.critical(owner, "Fatal Error", error);
            } else {
            	wm = WindowManager.warning(owner, "Warning", error);
            }
            wm.show();
            if (fatal) {
            	Stage currentStage = ContextManager.getPrimaryStage();
                currentStage.setOnHidden(e -> Platform.exit());
            }
        });
    }

    /* Static as we never need to objectify this method. */
    public static WindowManager dataDisplay(String title, Node content) {
    	Stage owner = ContextManager.getPrimaryStage();
    	Label titleLabel = new Label(title);
    	titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
    	if (content instanceof TextArea) {
    		((TextArea) content).setWrapText(true);
    	}
    	return new WindowManager(owner, titleLabel, content);
    }

/*
******************************************************************************
* These methods deal with creating and displaying the content boxes.         *
******************************************************************************
*/
	    /*
	     * Theoretically, if we move the the image extractor here, we could
	     * make changes like this to make it work and force the controller
	     * to validate a completed backup before running:

	      ArrayList<Path> MediaHier = new ArrayList<Path>();
	      MediaHier = BackupManager.getMediaHier(mediaLocation);
	      for (int i = 0; i < MediaHier.size(); i++) {
	          Path realMediaDir = MediaHier.get(i).getFileName();
	     * But also, we need to change some things to static and pass the
	     * path back and forth between these classes. Mark for a future
	     * "fix" or something.
	     */

    /* Ask to begin the extraction. */
    public static Boolean askYesNoBegin() {
    	Alert answer = new Alert(AlertType.CONFIRMATION);
    	answer.setTitle("Begin Yes/No?");
    	answer.setHeaderText("Begin the extraction process?");
    	answer.setContentText("The process will take several seconds"
    			+ " proceed with this action?");
    	Optional<ButtonType> res = answer.showAndWait();
    	return res.isPresent() && res.get() == ButtonType.OK;
    }

     /*
      * XXX: FileManager the report file.
      * XXX: Path ! exists, throw another error instead of silent fail.
      */
      public static void doShowCallHistory(Path restoreLocation) throws
      		ExceptionManager {

    	  Path callsURL = FilePairManager.CALLSHTML
    				  .getDecryptedPath(restoreLocation);
    	  try {
    		  Desktop.getDesktop().browse(callsURL.toUri());
    	  } catch (Exception e) {
    		  WindowManager.warning(ContextManager.getPrimaryStage(),
    				  "URI Error", "Error reading URI for call history data")
    		  		  .show();
    		  throw new ExceptionManager("Error reading URI for call history"
    				  + " data, system said: ", e, false);
    	  }
      }

      public static void doShowReport(Path restoreLocation) throws
      	ExceptionManager {

    	  Path reportURL = restoreLocation.resolve("PhoneReport.html");
    	  try {
    		  Desktop.getDesktop().browse(reportURL.toUri());
    	  } catch (Exception e) {
    		  WindowManager.warning(ContextManager.getPrimaryStage(),
    				  "URI Error", "Error reading URI for the Phone"
    				  + " report data.").show();
    		  throw new ExceptionManager("Error reading URI for the"
    				  + " phone report data, system said: ", e, false);
    	  }
      }

      public static void doShowContacts(Path restoreLocation) throws
      		ExceptionManager {

    	  Path contactsURL = FilePairManager.CONTACTSHTML
    				  .getDecryptedPath(restoreLocation);
    	  try {
    		  Desktop.getDesktop().browse(contactsURL.toUri());
    	  } catch (Exception e) {
    		  WindowManager.warning(ContextManager.getPrimaryStage(),
    				  "URI Error", "Error reading URI for contacts data")
    		  		  .show();
    		  throw new ExceptionManager("Error reading URI for Contacts"
    				  + " data, system said: ", e, false);
    	  }
      }

      public static void doShowMessageHistory(Path restoreLocation) throws
      		ExceptionManager {

    	  Path msgURL = FilePairManager.MESSAGESHTML
    				  .getDecryptedPath(restoreLocation);
    	  try {
    		  Desktop.getDesktop().browse(msgURL.toUri());
    	  } catch (Exception e) {
    		  WindowManager.warning(ContextManager.getPrimaryStage(),
    				  "URI Error", "Error reading URI for messages data")
    		  		  .show();
    		  throw new ExceptionManager("Error reading URI for Message data",
    				  e, false);
    	  }
      }

      public static void doShowSafariHistory(Path restoreLocation) throws
      		ExceptionManager {

    		Path safariURL = FilePairManager.SAFARIHTML
    				  .getDecryptedPath(restoreLocation);
    		try {
    			Desktop.getDesktop().browse(safariURL.toUri());
    	  } catch (Exception e) {
    		  WindowManager.warning(ContextManager.getPrimaryStage(),
    				  "URI Error", "Error reading URI for Safari data")
    		  		  .show();
    		  throw new ExceptionManager("Error reading URI for Safari data",
    				  e, false);
    	  }
      }

      public static void doShowVMList(Path restoreLocation) throws
      		ExceptionManager {
    		 Path vmURL = FilePairManager.VOICEMAILSHTML
    				  .getDecryptedPath(restoreLocation);

    		 try {
    		  Desktop.getDesktop().browse(vmURL.toUri());
    	  } catch (Exception e) {
    		  WindowManager.warning(ContextManager.getPrimaryStage(),
    				  "URI Error", "Error reading URI for voicemail");
    		  throw new ExceptionManager("Error reading Voicemail URI",
    				  e, false);
    	  }
      }

        public static void doShowAbout() throws ExceptionManager {

            try {
            	URI aboutURL = Paths.get("lib/About.html").toUri();
            	Desktop.getDesktop().browse(aboutURL);
            } catch (Exception e) {
            	throw new ExceptionManager("Unable to open about URI,"
            			+ " system said: ", e, false);
            }
        }

        public static void doShowHelp() throws ExceptionManager {
        	
        	try {
        		URI helpURL = Paths.get("lib/Help.html").toUri();
        		Desktop.getDesktop().browse(helpURL);
        	} catch (Exception e) {
        		throw new ExceptionManager("Unable to open help URI, system"
        			+ " said: ", e, false);
        }
    }
}
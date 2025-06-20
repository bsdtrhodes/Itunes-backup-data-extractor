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
 * NOTES:
 * o This could probably look a little nicer.
 */
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

	public static void main(String[] args) {
		/* Launch the application, it will do everything after starting. */
		Application.launch(args);
		/* Nothing left to do, we go or fail here! */
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		/* Store the primary stage for later use. */
		ContextManager.setPrimaryStage(primaryStage);

		/* Path "URL" to our window layout. */
		URL url = getClass().getResource("WindowLayout.fxml");
		if (url == null) {
			WindowManager.critical(primaryStage, "Critical Error",
					"Unable to locate WindowLayout.fxml!");
		}
		/* Create the main (VBox) pane from the file. */
		FXMLLoader loader = new FXMLLoader(url);
		VBox root = loader.load();

		/* Set the scene. */
		Scene scene = new Scene(root);
		URL surl = getClass().getResource("style.css");
		if (surl == null) {
			WindowManager.critical(primaryStage, "Critical Error",
					"Unable to locate style.css!");
		}
		String css = surl.toExternalForm();
		scene.getStylesheets().add(css);

		/* Set to the scene to stage. */
		primaryStage.setScene(scene);
		/* Can we get a better description up in here? */
		primaryStage.setTitle("iExtractor - An iTunes backup data extractor"
				+ " and password hash dumper.");
		primaryStage.show();
	}
}
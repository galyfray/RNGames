/**************************************************************************
 * RNGames, a software to record your inputs while playing.
 * Copyright (C) 2016  CORTIER Benoît, BOULMIER Jérôme
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *************************************************************************/

package fr.utbm.rngames.controller;

import fr.utbm.rngames.App;
import fr.utbm.rngames.Zipper;
import fr.utbm.rngames.event.EventDispatcher;
import fr.utbm.rngames.gamepad.GamepadMonitor;
import fr.utbm.rngames.gamepad.GamepadWriter;
import fr.utbm.rngames.keyboard.KeyboardWriter;
import fr.utbm.rngames.mouse.MouseWriter;
import fr.utbm.rngames.screen.ScreenMonitor;
import fr.utbm.rngames.screen.ScreenWriter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.DirectoryChooser;
import org.arakhne.afc.vmutil.locale.Locale;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class MainWindowController implements Initializable, CloseEventListener {

	private static final Logger LOG = Logger.getLogger(MainWindowController.class.getName());

	private KeyboardWriter kWriter;

	private MouseWriter mWriter;

	private ScreenWriter sWriter;

	private ScreenMonitor screenMonitor;

	private GamepadWriter pWriter;

	private GamepadMonitor gamepadMonitor;

	private final BooleanProperty startDisabled = new SimpleBooleanProperty(false);

	private final ExecutorService executorService = Executors.newFixedThreadPool(2);

	@FXML
	private TextField textAreaSaveDirectory;

	@FXML
	private Button buttonSelectDirectory;

	@FXML
	private TextField textAreaUserName;

	@FXML
	private TextField textAreaRecordName;

	@FXML
	private ToggleButton toggleButtonKeyboard;

	@FXML
	private ToggleButton toggleButtonMouse;

	@FXML
	private ToggleButton toggleButtonGamePad;

	@FXML
	private Button buttonStartRecording;

	@FXML
	private Button buttonStopRecording;

	// Reference to the main application
	private App app;

	// current date + record name
	private String fullRecordName;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.textAreaSaveDirectory.disableProperty().bind(this.startDisabled);
		this.textAreaSaveDirectory.disableProperty().bind(this.startDisabled);
		this.textAreaUserName.disableProperty().bind(this.startDisabled);
		this.textAreaRecordName.disableProperty().bind(this.startDisabled);
		this.buttonSelectDirectory.disableProperty().bind(this.startDisabled);
		this.toggleButtonGamePad.disableProperty().bind(this.startDisabled);
		this.toggleButtonKeyboard.disableProperty().bind(this.startDisabled);
		this.toggleButtonMouse.disableProperty().bind(this.startDisabled);
		this.buttonStartRecording.disableProperty().bind(this.startDisabled);
		this.buttonStopRecording.disableProperty().bind(this.startDisabled.not());

		EventDispatcher.getInstance().addListener(CloseEvent.class, this);

		/* Get the saveDirectory, username and recordName preference.
		 * The preference is read from the OS specific registry. If no such
		 * preference can be found, null is returned.
		 */
		Preferences prefs = Preferences.userNodeForPackage(App.class);

		String saveDirectory = prefs.get("saveDirectory", null); //$NON-NLS-1$
		if (saveDirectory != null) {
			this.textAreaSaveDirectory.setText(saveDirectory);
		}

		String username = prefs.get("username", null); //$NON-NLS-1$
		if (username != null) {
			this.textAreaUserName.setText(username);
		}

		String recordName = prefs.get("recordName", null); //$NON-NLS-1$
		if (recordName != null) {
			this.textAreaRecordName.setText(recordName);
		}
	}

	/**
	 * Is called by the main application to give a reference back to itself.
	 *
	 * @param app - application
	 */
	public void setApp(App app) {
		this.app = app;
	}

	/**
	 * Open a DirectoryChooser window.
	 */
	@FXML
	private void handleSelectFolder() {
		final DirectoryChooser chooser = new DirectoryChooser();

		chooser.setTitle(Locale.getString("directory.chooser.title")); //$NON-NLS-1$

		if (!this.textAreaSaveDirectory.getText().isEmpty()) {
			final File defaultDirectory = new File(this.textAreaSaveDirectory.getText());
			if (defaultDirectory.exists()) {
				chooser.setInitialDirectory(defaultDirectory);
			}
		}

		final File selectedDirectory = chooser.showDialog(this.app.getPrimaryStage());

		if (selectedDirectory != null) {
			this.textAreaSaveDirectory.setText(selectedDirectory.toString());
		}
	}

	@FXML
	private void handleStartRecording() {
		if (!isReadyToRecord()) {
			return;
		}

		this.startDisabled.set(true);

		if (this.toggleButtonKeyboard.isSelected()) {
			try {
				this.kWriter = new KeyboardWriter(new URL("file:///" + System.getProperty("java.io.tmpdir")
						+ File.separator
						+ Locale.getString(KeyboardWriter.class, "keyboard.file.name")));

				this.kWriter.start();
			} catch (IOException exception) {
				LOG.severe(exception.getMessage());
			}
		}

		if (this.toggleButtonMouse.isSelected()) {
			try {
				this.mWriter = new MouseWriter(new URL("file:///" + System.getProperty("java.io.tmpdir")
						+ File.separator
						+ Locale.getString(MouseWriter.class, "mouse.file.name")));

				this.mWriter.start();
			} catch (IOException exception) {
				LOG.severe(exception.getMessage());
			}

			try {
				this.sWriter = new ScreenWriter(new URL("file:///" + System.getProperty("java.io.tmpdir")
						+ File.separator
						+ Locale.getString(ScreenWriter.class, "screen.file.name")));

				this.sWriter.start();

				this.screenMonitor = new ScreenMonitor();
				this.executorService.execute(this.screenMonitor);
			} catch (IOException exception) {
				LOG.severe(exception.getMessage());
			}
		}

		if (this.toggleButtonGamePad.isSelected()) {
			try {
				this.pWriter = new GamepadWriter(new URL("file:///" + System.getProperty("java.io.tmpdir")
						+ File.separator
						+ Locale.getString(GamepadWriter.class, "gamepad.file.name")));

				this.pWriter.start();

				this.gamepadMonitor = new GamepadMonitor();
				this.executorService.execute(this.gamepadMonitor);
			} catch (IOException exception) {
				LOG.severe(exception.getMessage());
			}
		}
	}

	@FXML
	private void handleStopRecording() {
		this.startDisabled.set(false);
		stopAndZip();
	}

	@Override
	public void handleCloseEvent() {
		if (this.startDisabled.get()) {
			stopAndZip();
		}

		this.executorService.shutdown();

		/* Save saveDirectory, username and recordName in
		 * the OS specific registry.
		 */
		Preferences prefs = Preferences.userNodeForPackage(App.class);

		if (!this.textAreaSaveDirectory.getText().isEmpty()) {
			prefs.put("saveDirectory", this.textAreaSaveDirectory.getText()); //$NON-NLS-1$
		} else {
			prefs.remove("saveDirectory"); //$NON-NLS-1$
		}

		if (!this.textAreaUserName.getText().isEmpty()) {
			prefs.put("username", this.textAreaUserName.getText()); //$NON-NLS-1$
		} else {
			prefs.remove("username"); //$NON-NLS-1$
		}

		if (!this.textAreaRecordName.getText().isEmpty()) {
			prefs.put("recordName", this.textAreaRecordName.getText()); //$NON-NLS-1$
		} else {
			prefs.remove("recordName"); //$NON-NLS-1$
		}
	}

	/**
	 * Helper method to stop recording and zip data.
	 */
	private void stopAndZip() {
		try (Zipper zipper = new Zipper(new URL("file:///" + this.textAreaSaveDirectory.getText() //$NON-NLS-1$
				+ File.separator
				+ this.fullRecordName
				+ Zipper.EXTENSION_NAME))) {
			String logfileExtension = Locale.getString("logfile.extension"); //$NON-NLS-1$

			if (this.toggleButtonKeyboard.isSelected()) {
				this.kWriter.stop();
				zipper.addFile(this.kWriter.getFileLocation(),
						this.fullRecordName
						+ Locale.getString("logfile.keyboard.end") //$NON-NLS-1$
						+ logfileExtension);
			}

			if (this.toggleButtonMouse.isSelected()) {
				this.mWriter.stop();
				zipper.addFile(this.mWriter.getFileLocation(),
						this.fullRecordName
						+ Locale.getString("logfile.mouse.end") //$NON-NLS-1$
						+ logfileExtension);

				this.sWriter.stop();
				zipper.addFile(this.sWriter.getFileLocation(),
						this.fullRecordName
								+ Locale.getString("logfile.screen.end") //$NON-NLS-1$
								+ logfileExtension);
				this.screenMonitor.stop();
			}

			if (this.toggleButtonGamePad.isSelected()) {
				this.pWriter.stop();
				zipper.addFile(this.pWriter.getFileLocation(),
						this.fullRecordName
								+ Locale.getString("logfile.gamepad.end") //$NON-NLS-1$
								+ logfileExtension);
				this.gamepadMonitor.stop();
			}
		} catch (IOException exception) {
			LOG.severe(exception.getMessage());
		}
	}

	/**
	 * Check if the recording can be started.
	 *
	 * @return true if the recording can be started.
	 */
	private boolean isReadyToRecord() {
		final List<String> errorMessages = new ArrayList<>();

		if (this.textAreaSaveDirectory.getText().isEmpty()) {
			errorMessages.add(Locale.getString("error.no.save.directory")); //$NON-NLS-1$
		} else if (!new File(this.textAreaSaveDirectory.getText()).exists()) {
			errorMessages.add(Locale.getString("error.invalid.save.directory")); //$NON-NLS-1$
		}

		if (this.textAreaUserName.getText().isEmpty()) {
			errorMessages.add(Locale.getString("error.no.user.name")); //$NON-NLS-1$
		}

		if (this.textAreaRecordName.getText().isEmpty()) {
			errorMessages.add(Locale.getString("error.no.record.name")); //$NON-NLS-1$
		}

		if (!this.toggleButtonGamePad.isSelected()
				&& !this.toggleButtonKeyboard.isSelected()
				&& !this.toggleButtonMouse.isSelected()) {
			errorMessages.add(Locale.getString("error.no.device")); //$NON-NLS-1$
		}

		if (this.toggleButtonGamePad.isSelected() && GamepadMonitor.isNoGamepadFound()) {
			errorMessages.add(Locale.getString("error.no.gamepad.found")); //$NON-NLS-1$
		}

		if (!errorMessages.isEmpty()) {
			// Show the error message.
			final Alert alert = new Alert(AlertType.ERROR);
			alert.initOwner(this.app.getPrimaryStage());
			alert.setTitle(Locale.getString("alert.error.title")); //$NON-NLS-1$
			alert.setHeaderText(Locale.getString("alert.error.header")); //$NON-NLS-1$
			alert.setContentText(String.join("\n", errorMessages)); //$NON-NLS-1$);

			alert.showAndWait();

			return false;
		}

		DateFormat dateFormat = new SimpleDateFormat(Locale.getString("date.format")); //$NON-NLS-1$
		String currentDate = dateFormat.format(new Date());
		this.fullRecordName = currentDate + "." + this.textAreaUserName.getText() + "." + this.textAreaRecordName.getText();
		if (new File(this.textAreaSaveDirectory.getText() + File.separator
				+ this.fullRecordName + Zipper.EXTENSION_NAME).exists()) {
			// Show the confirmation message.
			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.getButtonTypes().set(0, ButtonType.YES);
			alert.getButtonTypes().set(1, ButtonType.NO);
			alert.initOwner(this.app.getPrimaryStage());
			alert.setTitle(Locale.getString("alert.record.already.existing.title")); //$NON-NLS-1$
			alert.setHeaderText(Locale.getString("alert.record.already.existing.header")); //$NON-NLS-1$
			alert.setContentText(Locale.getString("alert.record.already.existing.content")); //$NON-NLS-1$);

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.NO) {
				return false;
			}
		}

		return true;
	}
}

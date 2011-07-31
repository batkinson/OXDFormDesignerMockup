package org.openxdata.designer;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.Locale;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.io.FileList;
import org.apache.pivot.serialization.SerializationException;
import org.apache.pivot.serialization.Serializer;
import org.apache.pivot.util.Resources;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.apache.pivot.util.concurrent.TaskListener;
import org.apache.pivot.web.PostQuery;
import org.apache.pivot.web.QueryException;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.Clipboard;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.DialogCloseListener;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.DropAction;
import org.apache.pivot.wtk.HorizontalAlignment;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.Manifest;
import org.apache.pivot.wtk.MenuHandler;
import org.apache.pivot.wtk.MessageType;
import org.apache.pivot.wtk.Orientation;
import org.apache.pivot.wtk.Prompt;
import org.apache.pivot.wtk.TabPane;
import org.apache.pivot.wtk.TabPaneSelectionListener;
import org.apache.pivot.wtk.TaskAdapter;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.TextPane;
import org.apache.pivot.wtk.Theme;
import org.apache.pivot.wtk.TreeView;
import org.apache.pivot.wtk.VerticalAlignment;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtk.effects.OverlayDecorator;
import org.apache.pivot.wtk.text.Document;
import org.apache.pivot.wtk.text.PlainTextSerializer;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Question;
import org.openxdata.modelutils.ModelToXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point of the form designer application.
 * 
 * @author brent
 * 
 */
public class DesignerApp implements Application {

	public static String uploadHost = "http://oxdcirrus.appspot.com/upload/sampleform";
	public static final String LANGUAGE_KEY = "language";
	public static final String APPLICATION_KEY = "application";

	private Logger log = LoggerFactory.getLogger(DesignerApp.class);

	@BXML
	private TabPane tabPane;

	@BXML
	private TextPane formText;

	@BXML
	private TreeView designTree;

	@BXML
	private TreeView dynamicOptionTree;

	private Window window;

	private OverlayDecorator promptDecorator = new OverlayDecorator();

	private Locale locale;

	private Resources resources;

	public void startup(final Display display,
			final Map<String, String> properties) throws Exception {

		// Show window and then load asynchronously
		final Window loadingWindow = new Window();
		Label loadingLabel = new Label("Loading...");
		loadingLabel.getStyles().put("font", new Font("Arial", Font.BOLD, 24));
		loadingLabel.getStyles().put("horizontalAlignment",
				HorizontalAlignment.CENTER);
		loadingLabel.getStyles().put("verticalAlignment",
				VerticalAlignment.CENTER);

		loadingWindow.setContent(loadingLabel);
		loadingWindow.setMaximized(true);
		loadingWindow.open(display);

		Task<Void> startupTask = new Task<Void>() {
			@Override
			public Void execute() throws TaskExecutionException {
				try {
					String language = properties.get(LANGUAGE_KEY);
					locale = (language == null) ? Locale.getDefault()
							: new Locale(language);
					resources = new Resources(DesignerApp.class.getName(),
							locale);

					Theme theme = Theme.getTheme();
					Font font = theme.getFont();

					// Search for a font that can support the sample string
					String sampleResource = (String) resources.get("greeting");
					if (font.canDisplayUpTo(sampleResource) != -1) {
						Font[] fonts = GraphicsEnvironment
								.getLocalGraphicsEnvironment().getAllFonts();

						for (int i = 0; i < fonts.length; i++) {
							if (fonts[i].canDisplayUpTo(sampleResource) == -1) {
								theme.setFont(fonts[i].deriveFont(Font.PLAIN,
										12));
								break;
							}
						}
					}

					BXMLSerializer bxmlSerializer = new BXMLSerializer();

					// Install this object as "application" in the default
					// namespace
					bxmlSerializer.getNamespace().put(APPLICATION_KEY,
							DesignerApp.this);

					window = (Window) bxmlSerializer.readObject(
							DesignerApp.class.getResource("designer.bxml"),
							resources);

					// Apply the binding annotations to this object
					bxmlSerializer.bind(DesignerApp.this);

					// Apply the binding annotations to the menu handler
					MenuHandler designMenuHandler = designTree.getMenuHandler();
					if (designMenuHandler != null)
						bxmlSerializer.bind(designMenuHandler);

					MenuHandler dynOptMenuHandler = dynamicOptionTree
							.getMenuHandler();
					if (dynOptMenuHandler != null)
						bxmlSerializer.bind(dynOptMenuHandler);

					Label prompt = new Label("Drag or paste XML here");
					prompt.getStyles().put("horizontalAlignment",
							HorizontalAlignment.CENTER);
					prompt.getStyles().put("verticalAlignment",
							VerticalAlignment.CENTER);
					promptDecorator.setOverlay(prompt);
					formText.getDecorators().add(promptDecorator);
					designTree.getDecorators().add(promptDecorator);

					tabPane.getTabPaneSelectionListeners().add(
							new TabPaneSelectionListener.Adapter() {
								@Override
								public void selectedIndexChanged(
										TabPane tabPane,
										int previousSelectedIndex) {
									try {
										DesignerApp.this.updateXForm();
									} catch (Exception e) {
										log.error(
												"failed to update xform text",
												e);
									}
									super.selectedIndexChanged(tabPane,
											previousSelectedIndex);
								}
							});

					window.open(display);

					return null;
				} catch (Exception e) {
					throw new TaskExecutionException(
							"Failed to start application", e);
				}
			}
		};

		startupTask.execute(new TaskAdapter<Void>(new TaskListener<Void>() {
			@Override
			public void executeFailed(Task<Void> task) {
				Prompt.prompt(MessageType.ERROR, task.getFault().getMessage(),
						loadingWindow);
			}

			@Override
			public void taskExecuted(Task<Void> task) {
				loadingWindow.close();
			}
		}));
	}

	public void paste() {
		Manifest clipboardContent = Clipboard.getContent();

		if (clipboardContent != null && clipboardContent.containsText()) {
			String xml = null;
			try {
				xml = clipboardContent.getText();
				ByteArrayInputStream is = new ByteArrayInputStream(
						xml.getBytes());
				importFormDefinition(is);
			} catch (Exception exception) {
				log.error("exception while pasting", exception);
				Prompt.prompt(exception.getMessage(), window);
			}

			window.setTitle((String) resources.get("title"));
		}
	}

	public void upload() throws IOException, QueryException {

		BoxPane inputPane = new BoxPane(Orientation.VERTICAL);
		final TextInput uploadInput = new TextInput();
		uploadInput.setText(uploadHost);
		inputPane.add(new Label("Upload URL:"));
		inputPane.add(uploadInput);

		Alert.alert(MessageType.QUESTION, "Please enter the URL to upload to:",
				inputPane, window, new DialogCloseListener() {
					@Override
					public void dialogClosed(Dialog dialog, boolean modal) {

						try {
							uploadHost = uploadInput.getText();
							URL uploadUrl = new URL(uploadHost);
							int uploadPort = uploadUrl.getPort();
							String uploadPath = uploadUrl.getPath();
							final PostQuery query = new PostQuery(uploadUrl
									.getHost(), uploadPort == -1 ? 80
									: uploadPort, uploadPath, false);
							Form form = (Form) Sequence.Tree.get(
									designTree.getTreeData(),
									new Sequence.Tree.Path(0));
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							DataOutputStream dos = new DataOutputStream(baos);

							form.write(dos);

							byte[] contents = baos.toByteArray();
							query.setValue(contents);
							query.setSerializer(new Serializer<byte[]>() {
								public String getMIMEType(byte[] object) {
									return "application/octet-stream";
								};

								@Override
								public byte[] readObject(InputStream inputStream)
										throws IOException,
										SerializationException {
									// TODO Auto-generated method stub
									return null;
								}

								public void writeObject(byte[] object,
										java.io.OutputStream outputStream)
										throws IOException,
										SerializationException {
									outputStream.write(object);
								};
							});
							query.execute(new TaskListener<URL>() {

								@Override
								public void taskExecuted(
										org.apache.pivot.util.concurrent.Task<URL> task) {
									Prompt.prompt(MessageType.INFO,
											"Successfully uploaded form.",
											window);
								}

								@Override
								public void executeFailed(
										org.apache.pivot.util.concurrent.Task<URL> task) {
									Prompt.prompt(MessageType.ERROR,
											"Failed to upload form with status: "
													+ query.getStatus()
													+ " - "
													+ query.getFault()
															.getMessage(),
											window);
								}
							});
						} catch (IOException e) {
							Prompt.prompt(
									MessageType.ERROR,
									"Failed to write form to stream: "
											+ e.getMessage(), window);
							return;
						}
					}

				});

	}

	public Sequence.Tree.Path adjustPathForRemovedSibling(
			Sequence.Tree.Path dropped, Sequence.Tree.Path target) {

		// No adjustment needed unless we're moving a sibling of a descendant.
		if (target.getLength() < dropped.getLength())
			return target;

		int maxDepth = dropped.getLength() - 1;
		for (int depth = 0; depth < maxDepth; depth++) {
			if (dropped.get(depth) != target.get(depth))
				return target;
		}

		// If we get here, depth == maxDepth, just need to see if prior sibling
		if (dropped.get(maxDepth) < target.get(maxDepth)) {
			Sequence.Tree.Path adjustedTarget = new Sequence.Tree.Path(target);
			adjustedTarget.update(maxDepth, target.get(maxDepth) - 1);
			return adjustedTarget;
		}

		return target;
	}

	public DropAction drop(Manifest dragContent) {
		DropAction dropAction = null;

		try {
			if (dragContent.containsValue("targetPath")) {

				@SuppressWarnings("unchecked")
				List<Object> treeData = (List<Object>) designTree.getTreeData();

				Object draggedObject = dragContent.getValue("node");
				Sequence.Tree.Path draggedPath = (Sequence.Tree.Path) dragContent
						.getValue("path");

				Sequence.Tree.Path targetPath = (Sequence.Tree.Path) dragContent
						.getValue("targetPath");
				Object targetObject = Sequence.Tree.get(treeData, targetPath);
				Sequence.Tree.Path targetParentPath = new Sequence.Tree.Path(
						targetPath, targetPath.getLength() - 1);

				Sequence.Tree.remove(treeData, draggedObject);

				boolean isInsert = draggedObject.getClass() == targetObject
						.getClass()
						&& !(draggedObject instanceof Question && ((Question) targetObject)
								.isQuestionList());

				// Fix target when preceding sibling at any depth is removed.
				targetPath = adjustPathForRemovedSibling(draggedPath,
						targetPath);

				if (isInsert) {
					int insertLocation = targetPath
							.get(targetPath.getLength() - 1) + 1;
					designTree.setBranchExpanded(targetParentPath, true);
					Sequence.Tree.insert(treeData, draggedObject,
							targetParentPath, insertLocation);

				} else {
					designTree.setBranchExpanded(targetPath, true);
					Sequence.Tree.add(treeData, draggedObject, targetPath);
				}

			} else if (dragContent.containsFileList()) {
				FileList fileList = dragContent.getFileList();
				if (fileList.getLength() == 1) {
					File file = fileList.get(0);

					FileInputStream fileInputStream = null;
					try {
						try {
							fileInputStream = new FileInputStream(file);
							importFormDefinition(fileInputStream);
						} finally {
							if (fileInputStream != null) {
								fileInputStream.close();
							}
						}
					} catch (Exception exception) {
						log.error("exception while dropping", exception);
						Prompt.prompt(exception.getMessage(), window);
					}

					window.setTitle((String) resources.get("title") + "-"
							+ file.getName());

					dropAction = DropAction.COPY;
				} else {
					Prompt.prompt("Multiple files not supported.", window);
				}
			}
		} catch (IOException exception) {
			Prompt.prompt(exception.getMessage(), window);
		}

		return dropAction;
	}

	private void importFormDefinition(InputStream documentStream)
			throws IOException, SerializationException {

		// Remove prompt decorator
		if (promptDecorator != null) {
			formText.getDecorators().remove(promptDecorator);
			designTree.getDecorators().remove(promptDecorator);
			promptDecorator = null;
		}

		// Slurp input stream into String so we can parse twice
		BufferedReader br = new BufferedReader(new InputStreamReader(
				documentStream));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		String xform = sb.toString();
		setDesign(xform);
		updateXForm();
	}

	private void updateXForm() throws SerializationException, IOException {
		FormDef formDef = (FormDef) designTree.getTreeData().get(0);
		String exportedXform = ModelToXML.convert(formDef);
		PlainTextSerializer textSerializer = new PlainTextSerializer();
		Document doc = textSerializer
				.readObject(new StringReader(exportedXform));
		formText.setDocument(doc);
	}

	private void setDesign(String xformsXml) {
		Sequence.Tree.Path path;
		StringReader xmlReader = new StringReader(xformsXml);
		FormDef formDef = EpihandyXform.fromXform2FormDef(xmlReader);
		Form form = new Form(formDef);
		ArrayList<Form> designData = new ArrayList<Form>();
		designData.add(form);
		designTree.setTreeData(designData);

		path = new Sequence.Tree.Path(0);
		designTree.expandBranch(path);
		designTree.setSelectedPath(path);
	}

	public boolean shutdown(boolean optional) throws Exception {

		if (window != null)
			window.close();

		return false;
	}

	public void suspend() throws Exception {
		// TODO Auto-generated method stub
	}

	public void resume() throws Exception {
		// TODO Auto-generated method stub
	}
}

package com.example.dependency;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

@SpringBootApplication
public class ManagerApplication {

	public static void updateDependency(File pomFile, String groupId, String artifactId, String newVersion, boolean addIfNotExists, boolean deleteIfVersionNull) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pomFile);

			// Normalize XML structure
			doc.getDocumentElement().normalize();

			NodeList dependenciesList = doc.getElementsByTagName("dependencies");
			Element dependenciesElement = null;

			// Ensure the <dependencies> element exists
			if (dependenciesList.getLength() > 0) {
				dependenciesElement = (Element) dependenciesList.item(0);
			} else if (addIfNotExists) {
				dependenciesElement = doc.createElement("dependencies");
				doc.getDocumentElement().appendChild(dependenciesElement);
			}

			if (dependenciesElement == null) {
				// If there's no <dependencies> tag and we're not adding a new one, return early
				return;
			}

			NodeList dependencies = dependenciesElement.getElementsByTagName("dependency");
			boolean dependencyUpdated = false;
			boolean dependencyAdded = false;

			for (int i = 0; i < dependencies.getLength(); i++) {
				NodeList childNodes = dependencies.item(i).getChildNodes();
				String currentGroupId = null;
				String currentArtifactId = null;
				Node versionNode = null;

				for (int j = 0; j < childNodes.getLength(); j++) {
					if (childNodes.item(j).getNodeName().equals("groupId")) {
						currentGroupId = childNodes.item(j).getTextContent();
					} else if (childNodes.item(j).getNodeName().equals("artifactId")) {
						currentArtifactId = childNodes.item(j).getTextContent();
					} else if (childNodes.item(j).getNodeName().equals("version")) {
						versionNode = childNodes.item(j);
					}
				}

				// Update or delete dependency
				if (groupId.equals(currentGroupId) && artifactId.equals(currentArtifactId)) {
					if (versionNode != null) {
						versionNode.setTextContent(newVersion);
						dependencyUpdated = true;
					} else if (addIfNotExists) {
						Element versionElement = doc.createElement("version");
						versionElement.setTextContent(newVersion);
						dependencies.item(i).appendChild(versionElement);
						dependencyAdded = true;
					}
					break;
				}
			}

			// Add new dependency if not found
			if (!dependencyUpdated && addIfNotExists) {
				Element dependencyElement = doc.createElement("dependency");

				Element groupIdElement = doc.createElement("groupId");
				groupIdElement.setTextContent(groupId);
				dependencyElement.appendChild(groupIdElement);

				Element artifactIdElement = doc.createElement("artifactId");
				artifactIdElement.setTextContent(artifactId);
				dependencyElement.appendChild(artifactIdElement);

				Element versionElement = doc.createElement("version");
				versionElement.setTextContent(newVersion);
				dependencyElement.appendChild(versionElement);

				dependenciesElement.appendChild(dependencyElement);
				dependencyAdded = true;
			}

			// Save changes
			if (dependencyUpdated || dependencyAdded) {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(pomFile);
				transformer.transform(source, result);

				System.out.println("Updated pom.xml: " + pomFile.getAbsolutePath());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void processProject(File directory, String groupId, String artifactId, String newVersion, boolean addIfNotExists, boolean deleteIfVersionNull) {
		File[] files = directory.listFiles();
		if (files == null) return;

		for (File file : files) {
			if (file.isDirectory()) {
				processProject(file, groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull);
			} else if (file.isFile() && file.getName().equals("pom.xml")) {
				updateDependency(file, groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull);
			}
		}
	}

	public static void main(String[] args) {
		// Example usage
		String projectDirectory = "F:/server";
		String groupId = "org.springframework.boot";
		String artifactId = "spring-boot-starter-web";
		String newVersion = "BHAVIT";
		boolean addIfNotExists = true;
		boolean deleteIfVersionNull = false;  // This is not yet implemented; placeholder for future functionality

		processProject(new File(projectDirectory), groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull);
	}
}

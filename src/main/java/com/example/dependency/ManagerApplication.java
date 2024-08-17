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

			// Update dependencies in <dependencies>
			boolean updated = updateDependencies(doc, "dependencies", groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull);

			// Save changes if any updates occurred
			if (updated) {
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

	private static boolean updateDependencies(Document doc, String parentTagName, String groupId, String artifactId, String newVersion, boolean addIfNotExists, boolean deleteIfVersionNull) {
		NodeList parentList = doc.getElementsByTagName(parentTagName);
		if (parentList.getLength() == 0) {
			return false;
		}

		Node parentNode = parentList.item(0);
		NodeList dependencies = parentNode.getChildNodes();
		boolean dependencyUpdated = false;
		boolean dependencyAdded = false;
		boolean dependencyDeleted = false;

		for (int i = 0; i < dependencies.getLength(); i++) {
			Node node = dependencies.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE || !node.getNodeName().equals("dependency")) {
				continue;
			}

			NodeList childNodes = node.getChildNodes();
			String currentGroupId = null;
			String currentArtifactId = null;
			Node versionNode = null;

			for (int j = 0; j < childNodes.getLength(); j++) {
				Node child = childNodes.item(j);
				if (child.getNodeName().equals("groupId")) {
					currentGroupId = child.getTextContent();
				} else if (child.getNodeName().equals("artifactId")) {
					currentArtifactId = child.getTextContent();
				} else if (child.getNodeName().equals("version")) {
					versionNode = child;
				}
			}

			if (groupId.equals(currentGroupId) && artifactId.equals(currentArtifactId)) {
				if (deleteIfVersionNull && newVersion == null) {
					parentNode.removeChild(node);
					dependencyDeleted = true;
				} else if (versionNode != null) {
					versionNode.setTextContent(newVersion);
					dependencyUpdated = true;
				} else if (addIfNotExists) {
					Element versionElement = doc.createElement("version");
					versionElement.setTextContent(newVersion);
					node.appendChild(versionElement);
					dependencyAdded = true;
				}
				break;
			}
		}

		// Add new dependency if not found and not deleted
		if (!dependencyUpdated && !dependencyDeleted && addIfNotExists) {
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

			parentNode.appendChild(dependencyElement);
			dependencyAdded = true;
		}

		return dependencyUpdated || dependencyAdded || dependencyDeleted;
	}

	public static void excludeTransitiveDependency(File pomFile, String parentGroupId, String parentArtifactId, String excludeGroupId, String excludeArtifactId) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pomFile);

			// Normalize XML structure
			doc.getDocumentElement().normalize();

			NodeList dependenciesList = doc.getElementsByTagName("dependencies");
			if (dependenciesList.getLength() == 0) {
				return;
			}

			NodeList dependencies = dependenciesList.item(0).getChildNodes();
			boolean exclusionAdded = false;

			for (int i = 0; i < dependencies.getLength(); i++) {
				Node node = dependencies.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE || !node.getNodeName().equals("dependency")) {
					continue;
				}

				NodeList childNodes = node.getChildNodes();
				String currentGroupId = null;
				String currentArtifactId = null;
				Element exclusionsElement = null;

				for (int j = 0; j < childNodes.getLength(); j++) {
					Node child = childNodes.item(j);
					if (child.getNodeName().equals("groupId")) {
						currentGroupId = child.getTextContent();
					} else if (child.getNodeName().equals("artifactId")) {
						currentArtifactId = child.getTextContent();
					} else if (child.getNodeName().equals("exclusions")) {
						exclusionsElement = (Element) child;
					}
				}

				if (parentGroupId.equals(currentGroupId) && parentArtifactId.equals(currentArtifactId)) {
					if (exclusionsElement == null) {
						exclusionsElement = doc.createElement("exclusions");
						node.appendChild(exclusionsElement);
					}

					Element exclusionElement = doc.createElement("exclusion");

					Element excludeGroupIdElement = doc.createElement("groupId");
					excludeGroupIdElement.setTextContent(excludeGroupId);
					exclusionElement.appendChild(excludeGroupIdElement);

					Element excludeArtifactIdElement = doc.createElement("artifactId");
					excludeArtifactIdElement.setTextContent(excludeArtifactId);
					exclusionElement.appendChild(excludeArtifactIdElement);

					exclusionsElement.appendChild(exclusionElement);
					exclusionAdded = true;

					break;
				}
			}

			// Save changes
			if (exclusionAdded) {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(pomFile);
				transformer.transform(source, result);

				System.out.println("Excluded transitive dependency in pom.xml: " + pomFile.getAbsolutePath());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void removeExclusion(File pomFile, String parentGroupId, String parentArtifactId, String excludeGroupId, String excludeArtifactId) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pomFile);

			// Normalize XML structure
			doc.getDocumentElement().normalize();

			NodeList dependenciesList = doc.getElementsByTagName("dependencies");
			if (dependenciesList.getLength() == 0) {
				return;
			}

			NodeList dependencies = dependenciesList.item(0).getChildNodes();
			boolean exclusionRemoved = false;

			for (int i = 0; i < dependencies.getLength(); i++) {
				Node node = dependencies.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE || !node.getNodeName().equals("dependency")) {
					continue;
				}

				NodeList childNodes = node.getChildNodes();
				String currentGroupId = null;
				String currentArtifactId = null;
				Element exclusionsElement = null;

				for (int j = 0; j < childNodes.getLength(); j++) {
					Node child = childNodes.item(j);
					if (child.getNodeName().equals("groupId")) {
						currentGroupId = child.getTextContent();
					} else if (child.getNodeName().equals("artifactId")) {
						currentArtifactId = child.getTextContent();
					} else if (child.getNodeName().equals("exclusions")) {
						exclusionsElement = (Element) child;
					}
				}

				if (parentGroupId.equals(currentGroupId) && parentArtifactId.equals(currentArtifactId) && exclusionsElement != null) {
					NodeList exclusions = exclusionsElement.getChildNodes();
					for (int j = 0; j < exclusions.getLength(); j++) {
						Node exclusionNode = exclusions.item(j);
						if (exclusionNode.getNodeType() != Node.ELEMENT_NODE || !exclusionNode.getNodeName().equals("exclusion")) {
							continue;
						}

						NodeList exclusionChildren = exclusionNode.getChildNodes();
						String exclusionGroupId = null;
						String exclusionArtifactId = null;

						for (int k = 0; k < exclusionChildren.getLength(); k++) {
							Node exclusionChild = exclusionChildren.item(k);
							if (exclusionChild.getNodeName().equals("groupId")) {
								exclusionGroupId = exclusionChild.getTextContent();
							} else if (exclusionChild.getNodeName().equals("artifactId")) {
								exclusionArtifactId = exclusionChild.getTextContent();
							}
						}

						if (excludeGroupId.equals(exclusionGroupId) && excludeArtifactId.equals(exclusionArtifactId)) {
							exclusionsElement.removeChild(exclusionNode);
							exclusionRemoved = true;
							break;
						}
					}
				}

				if (exclusionRemoved && exclusionsElement != null && !exclusionsElement.hasChildNodes()) {
					node.removeChild(exclusionsElement);
				}
			}

			// Save changes
			if (exclusionRemoved) {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(pomFile);
				transformer.transform(source, result);

				System.out.println("Removed exclusion of transitive dependency in pom.xml: " + pomFile.getAbsolutePath());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void processProject(File directory, String groupId, String artifactId, String newVersion, boolean addIfNotExists, boolean deleteIfVersionNull, String excludeGroupId, String excludeArtifactId, boolean removeExclusion) {
		File[] files = directory.listFiles();
		if (files == null) return;

		for (File file : files) {
			if (file.isDirectory()) {
				processProject(file, groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull, excludeGroupId, excludeArtifactId, removeExclusion);
			} else if (file.isFile() && file.getName().equals("pom.xml")) {
				updateDependency(file, groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull);

				if (excludeGroupId != null && excludeArtifactId != null) {
					if (removeExclusion) {
						removeExclusion(file, groupId, artifactId, excludeGroupId, excludeArtifactId);
					} else {
						excludeTransitiveDependency(file, groupId, artifactId, excludeGroupId, excludeArtifactId);
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		// Example usage
		String projectDirectory = "F:/server";
		String groupId = "org.springframework.boot";
		String artifactId = "spring-boot-starter-web";
		String newVersion = "2.5.5";  // Set to null if you want to delete the dependency
		boolean addIfNotExists = true;
		boolean deleteIfVersionNull = true;
		String excludeGroupId = "org.springframework.boot";
		String excludeArtifactId = "spring-boot-starter-logging";
		boolean removeExclusion = false; // Set to true if you want to remove the exclusion

		processProject(new File(projectDirectory), groupId, artifactId, newVersion, addIfNotExists, deleteIfVersionNull, excludeGroupId, excludeArtifactId, removeExclusion);
	}
}

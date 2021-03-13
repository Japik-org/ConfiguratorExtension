package com.pro100kryto.server.extensions.configurator;

import com.pro100kryto.server.IServerControl;
import com.pro100kryto.server.Server;
import com.pro100kryto.server.logger.ILogger;
import com.pro100kryto.server.module.IModule;
import com.pro100kryto.server.service.IServiceControl;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class XmlConfigurator {
    private final IServerControl serverControl;
    private int maxCountRecursion;
    private File fileConfigs = null;
    private Document document = null;
    private final ILogger logger;

    public XmlConfigurator(IServerControl serverControl, int maxCountRecursion, ILogger logger) {
        this.serverControl = serverControl;
        this.maxCountRecursion = maxCountRecursion;
        this.logger = logger;
    }

    public void setMaxCountRecursion(int maxCountRecursion) {
        this.maxCountRecursion = maxCountRecursion;
    }

    public synchronized void loadFromFile(File fileConfigs) throws IOException, ParserConfigurationException, SAXException {
        this.fileConfigs = fileConfigs;
        reloadConfiguration();
    }

    public synchronized void reloadConfiguration() throws IOException, SAXException, ParserConfigurationException {
        if (!fileConfigs.exists()) throw new FileNotFoundException();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        document = dBuilder.parse(fileConfigs);
        document.normalize();
    }

    @Nullable
    public Document getDocument() {
        return document;
    }

    // -------------- services and modules

    public synchronized void execConfiguration() throws Throwable {
        if (document==null)
            throw new IllegalStateException("Configs not loaded");

        // server
        final Element elServer = document.getDocumentElement();

        // server - baseLibs
        if (elServer.getElementsByTagName("baseLibs").getLength() != 0){
            final NodeList nodeListBaseLibs = elServer.getElementsByTagName("baseLibs");
            for (int i = 0; i < nodeListBaseLibs.getLength(); i++) {
                if (nodeListBaseLibs.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
                final Element elBaseLibs = (Element) nodeListBaseLibs.item(i);
                final NodeList nodeListBaseLib = elBaseLibs.getElementsByTagName("baseLib");
                for (int j = 0; j < nodeListBaseLib.getLength(); j++) {
                    if (nodeListBaseLib.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                    final Element elBaseLib = (Element) nodeListBaseLib.item(j);
                    final String libPath = elBaseLib.getAttribute("path");
                    if (libPath.isEmpty()) continue;

                    final File fileBaseLib = createFileLib(libPath);
                    serverControl.addBaseLib(fileBaseLib.toURI().toURL());
                }
            }
        }

        // server - settings
        if (elServer.getElementsByTagName("settings").getLength()!=0)
        {
            final Element elServerSettings = (Element) elServer.getElementsByTagName("settings").item(0);
            final NodeList nodeListSetting = elServerSettings.getElementsByTagName("setting");

            for (int i = 0; i < nodeListSetting.getLength(); i++) {
                if (nodeListSetting.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
                final Element elementSetting = (Element) nodeListSetting.item(i);
                final String settingKey = elementSetting.getAttribute("key");
                final String settingVal = elementSetting.getAttribute("val");
                serverControl.setSetting(settingKey, settingVal);
            }
        }

        // server - services
        final Element elServices = (Element) elServer.getElementsByTagName("services").item(0);
        checkDelayAttr(elServices);

        // server - services - baseLibs
        if (elServices.getElementsByTagName("baseLibs").getLength() != 0){
            final NodeList nodeListBaseLibs = elServices.getElementsByTagName("baseLibs");
            for (int i = 0; i < nodeListBaseLibs.getLength(); i++) {
                if (nodeListBaseLibs.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
                final Element elBaseLibs = (Element) nodeListBaseLibs.item(i);
                final NodeList nodeListBaseLib = elBaseLibs.getElementsByTagName("baseLib");
                for (int j = 0; j < nodeListBaseLib.getLength(); j++) {
                    if (nodeListBaseLib.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                    final Element elBaseLib = (Element) nodeListBaseLib.item(j);
                    final String libPath = elBaseLib.getAttribute("path");
                    if (libPath.isEmpty()) continue;

                    final File fileBaseLib = createFileLib(libPath);
                    serverControl.addBaseLib(fileBaseLib.toURI().toURL());
                }
            }
        }

        // server -  services - service
        final NodeList nodeListService = elServices.getElementsByTagName("service");
        for (int i = 0; i < nodeListService.getLength(); i++) {
            if (nodeListService.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            final Element elService = (Element) nodeListService.item(i);
            final String serviceName = elService.getAttribute("name");
            final String serviceType = elService.getAttribute("type");

            checkDelayAttr(elService);

            final IServiceControl serviceControl =
                    serverControl.getServiceManager().createService(serviceType, serviceName);

            // server - services - service - baseLibs
            if (elService.getElementsByTagName("baseLibs").getLength() != 0){
                final NodeList nodeListBaseLibs = elService.getElementsByTagName("baseLibs");
                for (int j = 0; j < nodeListBaseLibs.getLength(); j++) {
                    if (nodeListBaseLibs.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                    final Element elBaseLibs = (Element) nodeListBaseLibs.item(j);
                    final NodeList nodeListBaseLib = elBaseLibs.getElementsByTagName("baseLib");
                    for (int k = 0; k < nodeListBaseLib.getLength(); k++) {
                        if (nodeListBaseLib.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                        final Element elBaseLib = (Element) nodeListBaseLib.item(k);
                        final String libPath = elBaseLib.getAttribute("path");
                        if (libPath.isEmpty()) continue;

                        final File fileBaseLib = createFileLib(libPath);
                        serviceControl.addBaseLib(fileBaseLib.toURI().toURL());
                    }
                }
            }

            // server - services - service - settings
            if (elService.getElementsByTagName("settings").getLength() != 0){
                final NodeList nodeListSettings = elService.getElementsByTagName("settings");
                for (int j = 0; j < nodeListSettings.getLength(); j++) {
                    if (nodeListSettings.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                    final Element elSettings = (Element) nodeListSettings.item(j);
                    final NodeList nodeListSetting = elSettings.getElementsByTagName("setting");
                    for (int k = 0; k < nodeListSetting.getLength(); k++) {
                        if (nodeListSetting.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                        final Element elSetting = (Element) nodeListSetting.item(k);
                        final String settingKey = elSetting.getAttribute("key");
                        final String settingVal = elSetting.getAttribute("val");
                        serviceControl.setSetting(settingKey, settingVal);
                    }
                }
            }

            // server - services - service - modules
            if (elService.getElementsByTagName("modules").getLength() != 0){
                final NodeList nodeListModules = elService.getElementsByTagName("modules");
                for (int j = 0; j < nodeListModules.getLength(); j++) {
                    if (nodeListModules.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                    final Element elModules = (Element) nodeListModules.item(j);
                    final NodeList nodeListModule = elModules.getElementsByTagName("module");
                    for (int k = 0; k < nodeListModule.getLength(); k++) {
                        if (nodeListModule.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                        final Element elModule = (Element) nodeListModule.item(k);

                        final IModule module = serviceControl.createModule(
                                elModule.getAttribute("type"),
                                elModule.getAttribute("name")
                        );

                        final NodeList nodeListModuleElements = elModule.getChildNodes();
                        for (int l = 0; l < nodeListModuleElements.getLength(); l++) {
                            if (nodeListModuleElements.item(l).getNodeType() != Node.ELEMENT_NODE) continue;
                            final Element elModuleEl = (Element) nodeListModuleElements.item(l);

                            if (elModuleEl.getTagName().equals("settings")) {

                                final Map<String, String> settings = new HashMap<>(nodeListModuleElements.getLength());
                                final NodeList nodeListSetting = elModuleEl.getChildNodes();

                                for (int m = 0; m < nodeListSetting.getLength(); m++) {
                                    if (nodeListSetting.item(m).getNodeType() != Node.ELEMENT_NODE) continue;
                                    final Element elSetting = (Element) nodeListSetting.item(m);
                                    settings.put(
                                            elSetting.getAttribute("key"),
                                            elSetting.getAttribute("val")
                                    );
                                }

                                module.setSettings(settings);
                            }
                            checkDelayAttr(elModule);
                        }
                        checkDelayAttr(elModules);
                    }
                }
            }

            checkOnLoadAttr(elService);
        }
        checkOnLoadAttr(elServices);
    }

    // ------------ actions

    public synchronized void execAction(String actionId) throws Throwable {
        execAction(actionId, 1);
    }

    private void execAction(String actionId, int recursionCount) throws Throwable{
        if (document==null)
            throw new IllegalStateException("Configs not loaded");

        if (recursionCount > maxCountRecursion)
            throw new IllegalStateException("Too many recursions");

        Element elActions = (Element) document.getDocumentElement().getElementsByTagName("actions").item(0);
        checkDelayAttr(elActions);
        NodeList nodeListAction = elActions.getChildNodes();

        for(int i=0; i < nodeListAction.getLength(); i++){
            if (nodeListAction.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            Element elAction = (Element) nodeListAction.item(i);
            String actionId_ = elAction.getAttribute("id");
            if (!actionId.equals(actionId_)) continue;

            checkDelayAttr(elAction);
            NodeList nodeListActionElements = elAction.getChildNodes();

            for(int j=0; j < nodeListActionElements.getLength(); j++){
                if (nodeListActionElements.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                Element elActionElement = (Element) nodeListActionElements.item(j);
                checkDelayAttr(elActionElement);

                switch (elActionElement.getTagName()){
                    case "action":
                        execAction(elActionElement.getAttribute("id"), recursionCount+1);
                        break;
                    case "service":
                        {
                            String actionMethod = elActionElement.getAttribute("method");
                            String serviceName = elActionElement.getAttribute("serviceName");
                            switch (actionMethod) {
                                case "start":
                                    serverControl.getServiceManager().getService(serviceName).start();
                                    break;
                                case "stop":
                                    serverControl.getServiceManager().getService(serviceName).stop(
                                            elActionElement.getAttribute("force").equals("true")
                                    );
                                    break;
                                default:
                                    throw new UnsupportedOperationException("Unsupported method '" + actionMethod + "'");
                            }
                        }
                        break;
                    case "module":
                        {
                            String actionMethod = elActionElement.getAttribute("method");
                            String serviceName = elActionElement.getAttribute("serviceName");
                            String moduleName = elActionElement.getAttribute("moduleName");
                            switch (actionMethod){
                                case "start":
                                    serverControl.getServiceManager()
                                            .getService(serviceName)
                                            .getModule(moduleName)
                                            .start();
                                    break;
                                case "stop":
                                    serverControl.getServiceManager()
                                            .getService(serviceName)
                                            .getModule(moduleName)
                                            .stop(elActionElement.getAttribute("force").equals("true"));
                                    break;
                                default:
                                    throw new UnsupportedOperationException("Unsupported method '" + actionMethod + "'");
                            }
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported tag '"+elActionElement.getTagName()+"'");
                }
            }
            break;
        }
    }

    // --------------

    private void checkOnLoadAttr(Element element) throws Throwable {
        if (element.hasAttribute("onLoad")) // TODO: rename attr to "actionAfter", add attr "actionBefore"
            execAction(element.getAttribute("onLoad"));
    }

    private void checkDelayAttr(Element element) throws InterruptedException {
        if (element.hasAttribute("delay")) {
            String delayStr = element.getAttribute("delay");
            int delay = Integer.parseInt(delayStr);
            Thread.sleep(delay);
        }
    }

    private File createFileLib(final String path){
        final File fileLib = new File(
                path.startsWith("file:") ?
                        path :
                        Server.getInstance().getWorkingPath() + File.separator
                                + "core" + File.separator
                                + path.replace("/", File.separator).replace("\\", File.separator));
        if (!fileLib.exists()) logger.writeWarn("\""+path + "\" not found");
        return fileLib;
    }
}

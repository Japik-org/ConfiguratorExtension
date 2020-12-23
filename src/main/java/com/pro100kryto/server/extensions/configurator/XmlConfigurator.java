package com.pro100kryto.server.extensions.configurator;

import com.pro100kryto.server.IServerControl;
import com.pro100kryto.server.module.IModule;
import com.pro100kryto.server.service.IServiceControl;
import com.sun.istack.Nullable;
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

    public XmlConfigurator(IServerControl serverControl, int maxCountRecursion) {
        this.serverControl = serverControl;
        this.maxCountRecursion = maxCountRecursion;
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

        Element elServer = document.getDocumentElement();
        Element elServices = (Element) elServer.getElementsByTagName("services").item(0);
        checkDelayAttr(elServices);
        NodeList nodeListService = elServices.getElementsByTagName("service");

        for (int i = 0; i < nodeListService.getLength(); i++) {
            Element elService = (Element) nodeListService.item(i);
            String serviceName = elService.getAttribute("name");
            String serviceType = elService.getAttribute("type");

            checkDelayAttr(elService);

            IServiceControl serviceControl =
                    serverControl.getServiceManager().createService(serviceType, serviceName);

            NodeList nodeListServiceElements = elService.getChildNodes();

            for (int j = 0; j < nodeListServiceElements.getLength(); j++) {
                if (nodeListServiceElements.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                Element elServiceElement = (Element) nodeListServiceElements.item(j);
                checkDelayAttr(elServiceElement);

                switch (elServiceElement.getTagName()){
                    case "modules":
                        {
                            NodeList nodeListModule = elServiceElement.getChildNodes();
                            for (int k = 0; k < nodeListModule.getLength(); k++) {
                                if (nodeListModule.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                                Element elModule = (Element) nodeListModule.item(k);

                                IModule module = serviceControl.createModule(
                                        elModule.getAttribute("type"),
                                        elModule.getAttribute("name")
                                );

                                NodeList nodeListModuleElements = elModule.getChildNodes();
                                for (int l = 0; l < nodeListModuleElements.getLength(); l++) {
                                    if (nodeListModuleElements.item(l).getNodeType() != Node.ELEMENT_NODE) continue;
                                    Element elModuleEl = (Element) nodeListModuleElements.item(l);

                                    if (elModuleEl.getTagName().equals("settings")) {

                                        Map<String, String> settings = new HashMap<>(nodeListModuleElements.getLength());
                                        NodeList nodeListSetting = elModuleEl.getChildNodes();

                                        for (int m = 0; m < nodeListSetting.getLength(); m++) {
                                            if (nodeListSetting.item(m).getNodeType() != Node.ELEMENT_NODE) continue;
                                            Element elSetting = (Element) nodeListSetting.item(m);
                                            settings.put(
                                                    elSetting.getAttribute("key"),
                                                    elSetting.getAttribute("val")
                                            );
                                        }

                                        module.setSettings(settings);
                                    }
                                }
                            }
                        }
                        break;
                    case "settings":
                        {
                            Map<String, String> settings = new HashMap<>();
                            // ...
                            //serviceControl.setSettings(settings);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported tag '"+elServiceElement.getTagName()+"'");
                }
                checkOnLoadAttr(elServiceElement);
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
        if (element.hasAttribute("onLoad"))
            execAction(element.getAttribute("onLoad"));
    }

    private void checkDelayAttr(Element element) throws InterruptedException {
        if (element.hasAttribute("delay")) {
            String delayStr = element.getAttribute("delay");
            int delay = Integer.parseInt(delayStr);
            Thread.sleep(delay);
        }
    }
}

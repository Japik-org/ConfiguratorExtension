package com.pro100kryto.server.extensions;

import com.pro100kryto.server.IServerControl;
import com.pro100kryto.server.Server;
import com.pro100kryto.server.StartStopStatus;
import com.pro100kryto.server.extension.IExtension;
import com.pro100kryto.server.extensions.configurator.XmlConfigurator;

import java.io.File;

public class ConfiguratorExtension implements IExtension {
    private final IServerControl serverControl;
    private final XmlConfigurator configurator;
    private StartStopStatus status = StartStopStatus.STOPPED;

    public ConfiguratorExtension(IServerControl serverControl) {
        this.serverControl = serverControl;
        configurator = new XmlConfigurator(serverControl, 15);
    }

    @Override
    public void start() throws Throwable {
        if (status!=StartStopStatus.STOPPED) throw new IllegalStateException("Is not stopped");
        if (Server.getInstance().getStatus() != StartStopStatus.STARTED) throw new IllegalStateException("Server is not started");
        status = StartStopStatus.STARTING;

        try{
            configurator.loadFromFile(
                    new File(serverControl.getWorkingPath() + File.separator +
                            "config.xml"));
            configurator.execConfiguration();

        } catch (Throwable throwable){
            status = StartStopStatus.STOPPED;
            throw throwable;
        }

        status = StartStopStatus.STARTED;
    }

    @Override
    public void stop(boolean force) throws Throwable {
        if (status==StartStopStatus.STOPPED) throw new IllegalStateException("Already stopped");
        status = StartStopStatus.STOPPING;

        // ...

        status = StartStopStatus.STOPPED;
    }

    @Override
    public StartStopStatus getStatus() {
        return status;
    }

    @Override
    public String getType() {
        return "Configurator";
    }

    @Override
    public void sendCommand(String command) throws Throwable {
        if (command.endsWith("execAction ")){
            String actionId = command.split(" ")[1];
            configurator.execAction(actionId);

        } else if (command.equals("execConfiguration")){
            configurator.execConfiguration();

        } else {
            throw new UnsupportedOperationException("Unknown command");
        }
    }
}

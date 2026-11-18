package dev.marulabs.sampleproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.marulabs.cadence.Cadence;
import dev.marulabs.cadence.CadenceDiagnosticsCommand;

@Plugin(id = "sampleproxy", name = "SampleProxy", version = "1.0.0")
public final class SampleProxyPlugin {
    private final ProxyServer proxy;

    @Inject
    public SampleProxyPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Permission nodes are sampleproxy.command.<command>. The proxy has no operators, so players need a
        // permission plugin to be granted the OP-default nodes; the console has every permission.
        Cadence.register(this,
                         proxy,
                         new SendCommand(),
                         new FindCommand(),
                         new CadenceDiagnosticsCommand("sampleproxy-diagnostics"));
    }
}

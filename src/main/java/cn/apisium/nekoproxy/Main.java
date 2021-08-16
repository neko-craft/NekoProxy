package cn.apisium.nekoproxy;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.*;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.*;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Plugin(name = "NekoProxy", version = "0.0.0")
@Description("A bukkit plugin can change the proxy.")
@Author("Shirasawa")
@Website("https://neko-craft.com")
@ApiVersion(ApiVersion.Target.v1_13)
@Commands(@Command(name = "nekoproxy", permission = "neko.proxy", desc = "Can use NekoProxy to change proxy.", usage = "/nekoproxy [uri]"))
@Permissions(@Permission(name = "neko.proxy"))
public final class Main extends JavaPlugin {
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        saveDefaultConfig();

        String proxy = getConfig().getString("proxy", "");
        org.bukkit.command.PluginCommand cmd = getServer().getPluginCommand("nekoproxy");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        if (proxy.isEmpty()) return;
        try {
            setProxy(new URI(proxy));
            getLogger().info("Set proxy to: " + proxy);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        URI uri;
        if (args.length == 0) uri = null;
        else try { uri = new URI(args[0]); } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
        try {
            setProxy(uri);
            sender.sendMessage("Set proxy to: " + (args.length == 0 ? null : args[0]));
            getConfig().set("proxy", args.length == 0 ? "" : args[0]);
            saveConfig();
        } catch (Throwable e) {
            e.printStackTrace();
            sender.sendMessage("Failed to set proxy!");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,org.bukkit.command.@NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    public static void setProxy(@Nullable final URI uri) throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Class<?> obc = Bukkit.getServer().getClass();
        Class<?> nms = obc.getMethod("getServer").invoke(Bukkit.getServer()).getClass();
        Object minecraftSessionService = nms.getMethod("getMinecraftSessionService")
                .invoke(nms.getMethod("getServer").invoke(null)),
                service = minecraftSessionService.getClass().getMethod("getAuthenticationService")
                        .invoke(minecraftSessionService);
        Field proxy = Class.forName("com.mojang.authlib.HttpAuthenticationService").getDeclaredField("proxy");
        proxy.setAccessible(true);
        proxy.set(service, uri == null ? Proxy.NO_PROXY : new Proxy(
                uri.getScheme().equalsIgnoreCase("socks") ? Proxy.Type.SOCKS : Proxy.Type.HTTP,
                new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 80 : uri.getPort())));
    }
}

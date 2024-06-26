package net.afyer.afybroker.server.command;

import com.alipay.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;
import net.afyer.afybroker.core.BrokerClientType;
import net.afyer.afybroker.core.message.SudoMessage;
import net.afyer.afybroker.server.BrokerServer;
import net.afyer.afybroker.server.plugin.Command;
import net.afyer.afybroker.server.plugin.TabExecutor;
import net.afyer.afybroker.server.proxy.BrokerClientProxy;
import net.afyer.afybroker.server.proxy.BrokerPlayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Nipuru
 * @since 2022/8/12 16:01
 */
@Slf4j
public class CommandSudo extends Command implements TabExecutor {

    private final BrokerServer brokerServer;

    public CommandSudo(BrokerServer brokerServer) {
        super("sudo");
        this.brokerServer = brokerServer;
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3) {
            log.info("/sudo <type> <player> <args>...");
            return;
        }

        BrokerClientType type;
        try {
            type = BrokerClientType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.info("invalid type!");
            return;
        }

        BrokerPlayer brokerPlayer = brokerServer.getBrokerPlayerManager().getPlayer(args[1]);
        if (brokerPlayer == null) {
            log.info("player not online!");
            return;
        }

        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            commandBuilder.append(args[i]).append(" ");
        }

        SudoMessage sudoMessage = new SudoMessage()
                .setType(type)
                .setPlayer(brokerPlayer.getName())
                .setCommand(commandBuilder.toString());

        BrokerClientProxy clientProxy = null;
        if (type == BrokerClientType.BUNGEE) {
            clientProxy = brokerPlayer.getBungeeClientProxy();
        } else if (type == BrokerClientType.BUKKIT) {
            clientProxy = brokerPlayer.getBukkitClientProxy();
        }

        if (clientProxy == null) {
            return;
        }

        try {
            clientProxy.oneway(sudoMessage);
        } catch (RemotingException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Iterable<String> onTabComplete(String[] args) {
        if (args.length == 1) {
            return Arrays.stream(BrokerClientType.values()).map(Enum::name).filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        } else if (args.length == 2) {
            return brokerServer.getBrokerPlayerManager().getPlayers().stream().map(BrokerPlayer::getName).filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

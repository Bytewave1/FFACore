package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MessageManager {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration config;
    private String prefix;

    public MessageManager(FFACore plugin) {
        reload(plugin);
    }

    public void reload(FFACore plugin) {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        prefix = config.getString("prefix", "<gradient:#00ff88:#00cc66><bold>FFA</bold></gradient> <dark_gray>| </dark_gray>");
    }

    public Component get(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, "<red>Missing message: " + key + "</red>");
        raw = raw.replace("{prefix}", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return mm.deserialize(raw);
    }

    public Component get(String key) {
        return get(key, null);
    }

    public Component getRandomKillMessage(String killer, String victim) {
        List<String> messages = config.getStringList("kill-messages");
        if (messages.isEmpty()) return Component.text(killer + " killed " + victim);
        String raw = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        raw = raw.replace("{prefix}", prefix)
                 .replace("{killer}", killer)
                 .replace("{victim}", victim);
        return mm.deserialize(raw);
    }

    public static Map<String, String> of(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}

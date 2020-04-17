/*
 * MIT License
 *
 * Copyright (c) 2020 Hasan Demirtaş
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.portlek.smartinventory;

import io.github.portlek.smartinventory.listeners.PluginDisableListener;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class SmartInventory {

    public static final Object LOCK = new Object();

    private static final Function<Plugin, Listener[]> LISTENERS = plugin ->
        new Listener[]{
            new PluginDisableListener()
        };

    private static final Queue<Plugin> PLUGIN_QUEUE = new ConcurrentLinkedQueue<>();

    public static void onPluginDisable(@NotNull final PluginDisableEvent event) {
        final Plugin peek = SmartInventory.PLUGIN_QUEUE.peek();
        if (peek != null && !peek.equals(event.getPlugin())) {
            synchronized (SmartInventory.LOCK) {
                SmartInventory.PLUGIN_QUEUE.remove(event.getPlugin());
                return;
            }
        }
        synchronized (SmartInventory.LOCK) {
            SmartInventory.PLUGIN_QUEUE.poll();
        }
        Optional.ofNullable(SmartInventory.PLUGIN_QUEUE.peek())
            .filter(Plugin::isEnabled)
            .ifPresent(SmartInventory::registerListeners);
    }

    public static void init(@NotNull final Plugin plugin) {
        if (SmartInventory.PLUGIN_QUEUE.isEmpty()) {
            SmartInventory.registerListeners(plugin);
        }
        synchronized (SmartInventory.LOCK) {
            SmartInventory.PLUGIN_QUEUE.add(plugin);
        }
    }

    private static void registerListeners(@NotNull final Plugin plugin) {
        Arrays.stream(SmartInventory.LISTENERS.apply(plugin)).forEach(listener ->
            Bukkit.getPluginManager().registerEvents(listener, plugin));
    }

}

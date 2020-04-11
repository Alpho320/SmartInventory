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

import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("unchecked")
public final class ClickableItem {

    /**
     * ClickableItem constant with no item and empty consumer.
     */
    public static final ClickableItem NONE = ClickableItem.empty(null);

    private final ItemStack item;

    private final Consumer<?> consumer;

    private final boolean legacy;

    private Predicate<Player> canSee;

    private Predicate<Player> canClick;

    private ItemStack notVisibleFallBackItem = null;

    private ClickableItem(final ItemStack item, final Consumer<?> consumer, final boolean legacy) {
        this.item = item;
        this.consumer = consumer;
        this.legacy = legacy;
    }

    /**
     * Creates a ClickableItem made of a given item and an empty consumer, thus
     * doing nothing when we click on the item.
     *
     * @param item the item
     * @return the created ClickableItem
     */
    public static ClickableItem empty(final ItemStack item) {
        return ClickableItem.from(item, data -> {
        });
    }

    /**
     * Creates a ClickableItem made of a given item and a given ItemClickData's consumer.
     *
     * @param item the item
     * @param consumer the consumer which will be called when the item is clicked
     * @return the created ClickableItem
     */
    public static ClickableItem from(final ItemStack item, final Consumer<ItemClickData> consumer) {
        return new ClickableItem(item, consumer, false);
    }

    /**
     * Creates a ClickableItem made of a given item and a given InventoryClickEvent's consumer.
     *
     * @param item the item
     * @param consumer the consumer which will be called when the item is clicked
     * @return the created ClickableItem
     * @deprecated Replaced by {@link ClickableItem#from(ItemStack, Consumer)}
     */
    @Deprecated
    public static ClickableItem of(final ItemStack item,
                                   final Consumer<InventoryClickEvent> consumer) {
        return new ClickableItem(item, consumer, true);
    }

    /**
     * Clones this ClickableItem using a different item.
     *
     * @param newItem the new item
     * @return the created ClickableItem
     */
    public ClickableItem clone(final ItemStack newItem) {
        final ClickableItem clone = new ClickableItem(newItem, this.consumer, this.legacy);
        clone.canSee = this.canSee;
        clone.canClick = this.canClick;
        return clone;
    }

    /**
     * Executes this ClickableItem's consumer using the given click data.
     *
     * @param data the data of the click
     */
    public void run(final ItemClickData data) {
        if ((this.canSee == null || this.canSee.test(data.getPlayer())) &&
            (this.canClick == null || this.canClick.test(data.getPlayer()))) {
            if (this.legacy) {
                if (data.getEvent() instanceof InventoryClickEvent) {
                    final InventoryClickEvent event = (InventoryClickEvent) data.getEvent();

                    this.run(event);
                }
            } else {
                final Consumer<ItemClickData> newConsumer = (Consumer<ItemClickData>) this.consumer;
                newConsumer.accept(data);
            }
        }
    }

    /**
     * Executes this ClickableItem's consumer using the given click event.
     *
     * @param e the click event
     * @deprecated This has been replaced by {@link ClickableItem#run(ItemClickData)}.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void run(final InventoryClickEvent e) {
        if ((this.canSee == null || this.canSee.test((Player) e.getWhoClicked())) &&
            (this.canClick == null || this.canClick.test((Player) e.getWhoClicked()))) {
            if (!this.legacy) {
                return;
            }
            final Consumer<InventoryClickEvent> legacyConsumer = (Consumer<InventoryClickEvent>) this.consumer;
            legacyConsumer.accept(e);
        }
    }

    /**
     * Returns the item contained in this ClickableItem disregarding the visibility test set
     * via {@link #canSee(Predicate, ItemStack)}.
     * <br>
     * <b>Warning:</b> The item can be {@code null}.
     *
     * @return the item, or {@code null} if there is no item
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Returns the item contained in this ClickableItem or the fallback item, if the player is not allowed to see
     * the item.
     * <br>
     * <b>Warning:</b> The item can be {@code null}.
     *
     * @param player The player to test against if he can see this item
     * @return the item, the fallback item when not visible to the player, or {@code null} if there is no item
     */
    public ItemStack getItem(final Player player) {
        if (this.canSee == null || this.canSee.test(player)) {
            return this.item;
        }
        return this.notVisibleFallBackItem;
    }

    /**
     * Sets a test to check if a player is allowed to see this item.
     * <br>
     * Note: If the player is not allowed to see the item, in the inventory this item will be empty.
     * <br>
     * Examples:
     * <ul>
     *     <li>{@code .canSee(player -> player.hasPermission("my.permission"))}</li>
     *     <li>{@code .canSee(player -> player.getHealth() >= 10)}</li>
     * </ul>
     *
     * @param canSee the test, if a player should be allowed to see this item
     * @return {@code this} for a builder-like usage
     * @see #canSee(Predicate, ItemStack) If you want to set a specific fallback item
     */
    public ClickableItem canSee(final Predicate<Player> canSee) {
        return this.canSee(canSee, null);
    }

    /**
     * Sets a test to check if a player is allowed to see this item.
     * <br>
     * If the player is <b>not</b> allowed to see the item, the fallback item will be used instead.
     * <br>
     * Note: If the player is not allowed to see the item, the on click handler will not be run
     * <br>
     * Examples:
     * <ul>
     *     <li>{@code .canSee(player -> player.hasPermission("my.permission"), backgroundItem)}</li>
     *     <li>{@code .canSee(player -> player.getHealth() >= 10, backgroundItem)}</li>
     * </ul>
     *
     * @param canSee the test, if a player should be allowed to see this item
     * @param fallBackItem the item that should be used, if the player is <b>not</b> allowed to see the item
     * @return {@code this} for a builder-like usage
     * @see #canSee(Predicate) If you want the slot to be empty
     */
    public ClickableItem canSee(final Predicate<Player> canSee, final ItemStack fallBackItem) {
        this.canSee = canSee;
        this.notVisibleFallBackItem = fallBackItem;
        return this;
    }

    /**
     * Sets a test to check if a player is allowed to click the item.
     * <br>
     * If a player is not allowed to click this item, the on click handler provided at creation will not be run
     *
     * @param canClick the test, if a player should be allowed to see this item
     * @return {@code this} for a builder-like usage
     */
    public ClickableItem canClick(final Predicate<Player> canClick) {
        this.canClick = canClick;
        return this;
    }

}

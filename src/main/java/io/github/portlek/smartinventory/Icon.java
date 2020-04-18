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

import io.github.portlek.smartinventory.event.ClickEvent;
import io.github.portlek.smartinventory.event.DragEvent;
import io.github.portlek.smartinventory.event.IconEvent;
import io.github.portlek.smartinventory.event.SmartEvent;
import io.github.portlek.smartinventory.icon.BasicIcon;
import io.github.portlek.smartinventory.old.content.InventoryContents;
import io.github.portlek.smartinventory.target.BasicTarget;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Icon {

    @NotNull
    static Icon from(@NotNull final ItemStack item) {
        return new BasicIcon(item);
    }

    @SafeVarargs
    @NotNull
    static Icon click(@NotNull final ItemStack item, @NotNull final Consumer<ClickEvent> consumer,
                      @NotNull final Requirement<ClickEvent>... requirements) {
        return new BasicIcon(item)
            .whenclick(consumer, requirements);
    }

    @SafeVarargs
    @NotNull
    static Icon drag(@NotNull final ItemStack item, @NotNull final Consumer<DragEvent> consumer,
                     @NotNull final Requirement<DragEvent>... requirements) {
        return new BasicIcon(item)
            .whendrag(consumer, requirements);
    }

    @NotNull
    static Icon cancel(@NotNull final ItemStack item) {
        return new BasicIcon(item)
            .wheninteract(SmartEvent::cancel);
    }

    @NotNull
    static Icon empty() {
        return new BasicIcon(new ItemStack(Material.AIR));
    }

    @NotNull
    ItemStack calculateItem();

    @NotNull
    ItemStack calculateItem(@NotNull InventoryContents contents);

    <T extends IconEvent> void accept(@NotNull T event);

    @NotNull
    default Icon wheninteract(@NotNull final Consumer<IconEvent> consumer) {
        return this.wheninteract(consumer, new Requirement[0]);
    }

    @NotNull
    default Icon wheninteract(@NotNull final Consumer<IconEvent> consumer,
                              @NotNull final Requirement<IconEvent>... requirements) {
        return this.target(IconEvent.class, consumer, requirements);
    }

    @NotNull
    default Icon whendrag(@NotNull final Consumer<DragEvent> consumer) {
        return this.whendrag(consumer, new Requirement[0]);
    }

    @NotNull
    default Icon whendrag(@NotNull final Consumer<DragEvent> consumer,
                          @NotNull final Requirement<DragEvent>... requirements) {
        return this.target(DragEvent.class, consumer, requirements);
    }

    @NotNull
    default Icon whenclick(@NotNull final Consumer<ClickEvent> consumer) {
        return this.whenclick(consumer, new Requirement[0]);
    }

    @NotNull
    default Icon whenclick(@NotNull final Consumer<ClickEvent> consumer,
                           @NotNull final Requirement<ClickEvent>... requirements) {
        return this.target(ClickEvent.class, consumer, requirements);
    }

    @NotNull
    default <T extends IconEvent> Icon target(@NotNull final Class<T> clazz, @NotNull final Consumer<T> consumer,
                                              @NotNull final Requirement<T>... requirements) {
        return this.target(new BasicTarget<>(clazz, consumer, requirements));
    }

    @NotNull <T extends IconEvent> Icon target(@NotNull Target<T> target);

    @NotNull Icon targets(@NotNull Collection<Target<? extends IconEvent>> targets);

    @NotNull
    Icon canSee(@NotNull Predicate<InventoryContents> predicate);

    @NotNull
    Icon canUse(@NotNull Predicate<InventoryContents> predicate);

    @NotNull
    Icon fallback(@NotNull ItemStack fallback);

    @NotNull
    Icon clone(@NotNull ItemStack newitem);

}

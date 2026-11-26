package eu.praxios.blocklords.ui;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiActionTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void defaultActionIsUsedForBothClicks() {
        AtomicInteger calls = new AtomicInteger();
        UiAction action = UiAction.of(Material.STONE, "Test", calls::incrementAndGet);
        action.forClick(false).run();
        action.forClick(true).run();
        assertEquals(2, calls.get());
    }

    @Test
    void rightClickUsesDedicatedAction() {
        AtomicInteger left = new AtomicInteger();
        AtomicInteger right = new AtomicInteger();
        UiAction action = UiAction.of(Material.STONE, "Test", left::incrementAndGet, right::incrementAndGet);
        action.forClick(false).run();
        action.forClick(true).run();
        assertEquals(1, left.get());
        assertEquals(1, right.get());
    }
}

package org.onebeastchris.extension;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.bedrock.camera.*;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandExecutor;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.awt.*;
import java.util.UUID;

public class CameraApiExtensionTest implements Extension {

    @Subscribe
    public void onEnable(GeyserPostInitializeEvent event) {
        logger().info("Camera API Extension Test enabled");
        logger().info("Usage: /apitest fade (stop)");
        logger().info("Usage: /apitest position (stop)");
        logger().info("Usage: /apitest input (movement, camera, both, unlock)");
        logger().info("Usage: /apitest perspective (first, third, third_front, stop, free)");
    }

    @Subscribe
    public void onCommands(GeyserDefineCommandsEvent event) {
        logger().info("Registering camera test commands");
        event.register(Command.builder(this)
                .name("fade")
                .bedrockOnly(true)
                .executableOnConsole(true)
                .description("camera fade test (random color/fade time)")
                .executor(this::cameraFade)
                .source(CommandSource.class)
                .build());

        event.register(Command.builder(this)
                .name("position")
                .bedrockOnly(true)
                .executableOnConsole(true)
                .description("camera position test (random position/facing/ease type)")
                .executor(this::cameraPosition)
                .source(CommandSource.class)
                .build());

        event.register(Command.builder(this)
                .name("input")
                .bedrockOnly(true)
                .executableOnConsole(true)
                .description("input locks test")
                .executor(this::inputLocksTest)
                .source(CommandSource.class)
                .build());

        event.register(Command.builder(this)
                .name("perspective")
                .bedrockOnly(true)
                .executableOnConsole(true)
                .description("force camera perspective")
                .executor(this::perspectiveTest)
                .source(CommandSource.class)
                .build());

        event.register(Command.builder(this)
                .name("a")
                .source(CommandSource.class)
                .executor((source, command, args) -> {
                    source.sendMessage("You can stack position and fade instructions!");
                    GeyserConnection connection = (GeyserConnection) source;
                    connection.camera().sendCameraPosition(
                            CameraPosition.builder()
                                    .position(connection.entities().playerEntity().position().add(Vector3f.from(10, 10, 10)))
                                    .cameraFade(CameraFade.builder()
                                            .fadeInSeconds(1)
                                            .fadeHoldSeconds(1)
                                            .fadeOutSeconds(1)
                                            .color(Color.RED)
                                            .build())
                                    .easeType(CameraEaseType.LINEAR)
                                    .renderPlayerEffects(true)
                                    .easeSeconds(2)
                                    .facingPosition(connection.entities().playerEntity().position())
                                    .build()
                    );
                })
                .build());
    }

    private void cameraFade(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0 && args[0].equalsIgnoreCase("stop")) {
            for (GeyserConnection connection : geyserApi().onlineConnections()) {
                commandSource.sendMessage("Clearing camera instructions");
                connection.camera().clearCameraInstructions();
            }
            return;
        }

        int blue = (int) (Math.random() * 255);
        int green = (int) (Math.random() * 255);
        int red = (int) (Math.random() * 255);

        float fadeIn, hold, fadeOut;
        do {
            fadeIn = 0.5f + (float) (Math.random() * 9.5);
            hold = 0.5f + (float) (Math.random() * 9.5);
            fadeOut = 0.5f + (float) (Math.random() * 9.5);
        } while ((fadeIn + hold + fadeOut) >= 10);

        CameraFade fade = CameraFade.builder()
                .color(new Color(red, green, blue))
                .fadeInSeconds(fadeIn)
                .fadeHoldSeconds(hold)
                .fadeOutSeconds(fadeOut)
                .build();

        commandSource.sendMessage("Fading to " + red + ", " + green + ", " + blue + " over " + (fadeIn + hold + fadeOut) + " seconds");

        for (GeyserConnection connection : geyserApi().onlineConnections()) {
            connection.camera().sendCameraFade(fade);
        }
    }

    private void cameraPosition(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                for (GeyserConnection connection : geyserApi().onlineConnections()) {
                    commandSource.sendMessage("Clearing camera instructions");
                    connection.camera().clearCameraInstructions();
                }
            }
        }

        CameraEaseType easeType = CameraEaseType.values()[(int) (Math.random() * CameraEaseType.values().length)];
        float easeDuration = (float) (Math.random() * 10);

        boolean renderPlayerEffects = Math.random() > 0.5;
        boolean playerPositionForAudio = Math.random() > 0.5;

        boolean facing = Math.random() > 0.5;

        CameraPosition.Builder position = CameraPosition.builder()
                .playerPositionForAudio(playerPositionForAudio)
                .renderPlayerEffects(renderPlayerEffects)
                .easeType(easeType)
                .easeSeconds(easeDuration);

        for (GeyserConnection connection : geyserApi().onlineConnections()) {
            Vector3f playerPosition = connection.entities().playerEntity().position();
            Vector3f cameraPosition = Vector3f.from(playerPosition.getX() + 10, playerPosition.getY() + 10, playerPosition.getZ() + 10);

            if (facing) {
                position.facingPosition(playerPosition);
            } else {
                position.rotationX((int) (Math.random() * 180) - 90)
                        .rotationY((int) (Math.random() * 360));
            }

            connection.camera().sendCameraPosition(position
                    .position(cameraPosition)
                    .build());

            // Reset camera after 15 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connection.camera().clearCameraInstructions();
            }).start();

            commandSource.sendMessage("Sent camera position: \n" +
                    "easeType: " + easeType + "\n" +
                    "easeDuration: " + easeDuration + "\n" +
                    "renderPlayerEffects: " + renderPlayerEffects + "\n" +
                    "playerPositionForAudio: " + playerPositionForAudio + "\n" +
                    "position: " + cameraPosition + "\n" +
                    (facing ? "facing position: " + position.build().facingPosition()
                            : "rotationX: " + position.build().rotationX() + "\n" +
                            "rotationY: " + position.build().rotationY() + "\n") +
                    "Resetting camera in 15 seconds");
        }
    }

    private void inputLocksTest(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                for (GeyserConnection connection : geyserApi().onlineConnections()) {
                    commandSource.sendMessage("Clearing camera instructions");
                    connection.camera().clearCameraInstructions();
                }
                return;
            }

            switch (args[0]) {
                case "movement":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Locking movement");
                        connection.entities().lockMovement(true, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    }
                    break;
                case "camera":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Locking camera");
                        connection.camera().lockCamera(true, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    }
                    break;
                case "both":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Locking movement and camera");
                        connection.camera().lockCamera(true, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                        connection.entities().lockMovement(true, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    }
                    break;
                case "unlock":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Unlocking movement and camera");
                        connection.camera().lockCamera(false, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                        connection.entities().lockMovement(false, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    }
                    break;
            }
        } else {
            commandSource.sendMessage("unlocking all locks");
            commandSource.sendMessage("Valid args: movement, camera, both, unlock");
            for (GeyserConnection connection : geyserApi().onlineConnections()) {
                connection.camera().lockCamera(false, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                connection.entities().lockMovement(false, UUID.fromString("00000000-0000-0000-0000-000000000000"));
            }
        }
    }

    private void perspectiveTest(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                for (GeyserConnection connection : geyserApi().onlineConnections()) {
                    commandSource.sendMessage("Clearing camera instructions");
                    connection.camera().clearCameraInstructions();
                }
                return;
            }

            switch (args[0]) {
                case "first":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to first person");
                        connection.camera().forceCameraPerspective(CameraPerspective.FIRST_PERSON);
                    }
                    break;
                case "third":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to free");
                        connection.camera().forceCameraPerspective(CameraPerspective.THIRD_PERSON);
                    }
                    break;
                case "third_front":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to third person front");
                        connection.camera().forceCameraPerspective(CameraPerspective.THIRD_PERSON_FRONT);
                    }
                    break;
                case "free":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to free");
                        Vector3f camPos = connection.entities().playerEntity().position();
                        connection.camera().sendCameraPosition(CameraPosition.builder()
                                        .position(camPos.add(0, 10, 0))
                                        .facingPosition(camPos)
                                .build());
                    }
                    break;
                default:
                    commandSource.sendMessage("Invalid perspective");
                    commandSource.sendMessage("Valid perspectives: first, third, third_front, stop, free");
                    break;
            }
        } else {
            commandSource.sendMessage("Invalid perspective");
            commandSource.sendMessage("Valid perspectives: first, third, third_front, stop, free");
        }
    }
}

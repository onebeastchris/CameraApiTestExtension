package org.onebeastchris.extension;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.bedrock.camera.CameraEaseType;
import org.geysermc.geyser.api.bedrock.camera.CameraFade;
import org.geysermc.geyser.api.bedrock.camera.CameraPerspective;
import org.geysermc.geyser.api.bedrock.camera.CameraPosition;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Position;

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
    }

    private void cameraFade(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0 && args[0].equalsIgnoreCase("stop")) {
            for (GeyserConnection connection : geyserApi().onlineConnections()) {
                commandSource.sendMessage("Clearing camera instructions");
                connection.clearCameraInstructions();
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
                .red(red)
                .green(green)
                .blue(blue)
                .fadeInSeconds(fadeIn)
                .holdSeconds(hold)
                .fadeOutSeconds(fadeOut)
                .build();

        commandSource.sendMessage("Fading to " + red + ", " + green + ", " + blue + " over " + (fadeIn + hold + fadeOut) + " seconds");

        for (GeyserConnection connection : geyserApi().onlineConnections()) {
            connection.sendCameraFade(fade);
        }
    }

    private void cameraPosition(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0 && args[0].equalsIgnoreCase("stop")) {
            for (GeyserConnection connection : geyserApi().onlineConnections()) {
                commandSource.sendMessage("Clearing camera instructions");
                connection.clearCameraInstructions();
            }
            return;
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
                .easeDuration(easeDuration);

        for (GeyserConnection connection : geyserApi().onlineConnections()) {
            Position playerPosition = connection.playerEntity().position();
            Position cameraPosition = new Position(playerPosition.x() + 10, playerPosition.y() + 10, playerPosition.z() + 10);

            if (facing) {
                position.facingPosition(playerPosition);
            } else {
                position.rotationX((int) (Math.random() * 180) - 90)
                        .rotationY((int) (Math.random() * 360));
            }

            connection.sendCameraPosition(position
                    .position(cameraPosition)
                    .build());

            // Reset camera after 15 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connection.clearCameraInstructions();
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
                    connection.clearCameraInstructions();
                }
                return;
            }

            switch (args[0]) {
                case "movement":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Locking movement");
                        connection.lockInputs(false, true);
                    }
                    break;
                case "camera":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Locking camera");
                        connection.lockInputs(true, false);
                    }
                    break;
                case "both":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Locking movement and camera");
                        connection.lockInputs(true, true);
                    }
                    break;
                case "unlock":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Unlocking movement and camera");
                        connection.lockInputs(false, false);
                    }
                    break;
            }
        } else {
            commandSource.sendMessage("unlocking all locks");
            commandSource.sendMessage("Valid args: movement, camera, both, unlock");
            for (GeyserConnection connection : geyserApi().onlineConnections()) {
                connection.unlockInputs();
            }
        }
    }

    private void perspectiveTest(CommandSource commandSource, Command command, String[] args) {
        if (args != null && args.length != 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                for (GeyserConnection connection : geyserApi().onlineConnections()) {
                    commandSource.sendMessage("Clearing camera instructions");
                    connection.clearCameraInstructions();
                }
                return;
            }

            switch (args[0]) {
                case "first":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to first person");
                        connection.forceCameraPerspective(CameraPerspective.FIRST_PERSON);
                    }
                    break;
                case "third":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to free");
                        connection.forceCameraPerspective(CameraPerspective.THIRD_PERSON);
                    }
                    break;
                case "third_front":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to third person front");
                        connection.forceCameraPerspective(CameraPerspective.THIRD_PERSON_FRONT);
                    }
                    break;
                case "free":
                    for (GeyserConnection connection : geyserApi().onlineConnections()) {
                        commandSource.sendMessage("Setting camera perspective to free");
                        Position camPos = connection.playerEntity().position();
                        connection.sendCameraPosition(CameraPosition.builder()
                                        .position(new Position(camPos.x() + 10, camPos.y() + 10, camPos.z()))
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

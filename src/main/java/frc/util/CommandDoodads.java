package frc.util;


import java.util.Set;
import java.util.function.Supplier;

import edu.wpi.first.wpilibj2.command.Command;
import static edu.wpi.first.wpilibj2.command.Commands.*;

public final class CommandDoodads {
    private CommandDoodads() {}

	/**
	 * Waits a moment before printing (???). The code is kind of wild, would recommend a peek
	 * @param stringSup - The String to print after a moment
	 */
    public static Command printLater(Supplier<String> stringSup) {
		return defer(() -> {
			return print(stringSup.get());
		}, Set.of());
	}
    
}

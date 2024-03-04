package frc.robot.auton;

import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.shooter.Shooter;
import frc.util.logging.WaltLogger;
import frc.util.logging.WaltLogger.DoubleLogger;
import frc.robot.subsystems.Superstructure;

import static frc.robot.Constants.AimK.kPodiumAngle;
import static frc.robot.Constants.AimK.kSubwooferAngle;

import com.pathplanner.lib.auto.AutoBuilder;

public final class AutonFactory {
	public static final DoubleLogger log_state = WaltLogger.logDouble(
		"Auton", "currentCmd", PubSubOption.sendAll(true));
	public static double currentState = 0;

	public static Command oneMeter() {
		return AutoBuilder.followPath(Paths.oneMeter);
	}

	public static Command simpleThing(Swerve swerve) {
		var resetPose = swerve.resetPose(Paths.simpleThing);

		return Commands.sequence(
			resetPose, 
			AutoBuilder.followPath(Paths.simpleThing)
		);
	}

	public static Command leave(Superstructure superstructure, Shooter shooter, Swerve swerve) {
		var resetPose = swerve.resetPose(Paths.leave).asProxy();
		var shoot = shoot(superstructure, shooter);

		var pathFollow = AutoBuilder.followPath(Paths.leave).asProxy();
		return Commands.sequence(
			Commands.parallel(
				resetPose,
				Commands.sequence(incrementState(), shoot)
			),
			incrementState(),
			pathFollow
		);
	}

	public static Command altTwoPc(Superstructure superstructure, Shooter shooter, Swerve swerve) {
		var resetPose = swerve.resetPose(Paths.altTwo).asProxy();
		var pathFollow = AutoBuilder.followPath(Paths.altTwo).asProxy();
		var preloadShot = shoot(superstructure, shooter);
		var intake = superstructure.autonIntakeCmd().asProxy();
		var swerveAim = swerve.aim(0).asProxy();
		var aimAndSpinUp = superstructure.aimAndSpinUp(() -> kPodiumAngle, false, true, true).until(superstructure.stateTrg_idle);
		var secondShot = superstructure.autonShootReq().asProxy();

		return Commands.sequence(
			Commands.parallel(
				resetPose, 
				preloadShot),
			Commands.parallel(
				Commands.print("going to intake now"),
				intake,
				pathFollow
			),
			Commands.parallel(
				Commands.sequence(
					Commands.waitUntil(superstructure.stateTrg_noteReady),
					Commands.print("note ready")
				),
				Commands.sequence(
					swerveAim.withTimeout(0.1),
					Commands.print("aimed")
				)
			),
			Commands.print("note ready & aimed"),
			Commands.parallel(
				aimAndSpinUp,
				secondShot
			)
		);
	}

	public static Command twoPc(Superstructure superstructure, Shooter shooter, Swerve swerve) {
		var leave = leave(superstructure, shooter, swerve);
		var intake = superstructure.autonIntakeCmd().asProxy();
		var pathFollow = AutoBuilder.followPath(Paths.twoPc).asProxy();
		var aimAndSpinUp = superstructure.aimAndSpinUp(() -> kSubwooferAngle, false, false, false);
		var shoot = Commands.runOnce(() -> superstructure.forceStateToShooting()).asProxy();

		return Commands.sequence(
			leave,
			Commands.parallel(
				intake,
				pathFollow,
				Commands.sequence(
					Commands.waitUntil(superstructure.stateTrg_noteReady),
					Commands.parallel(
						aimAndSpinUp,
						Commands.sequence(
							Commands.waitUntil(() -> pathFollow.isFinished()), 
							shoot
						)
					)
				)
			)
		);
	}

	public static Command threePc(Superstructure superstructure, Shooter shooter, Swerve swerve) {
		/* everything from 2 piece */
		var resetPose = swerve.resetPose(Paths.altTwo).asProxy();
		var pathFollow = AutoBuilder.followPath(Paths.altTwo).asProxy();
		var preloadShot = shoot(superstructure, shooter);
		var intake = superstructure.autonIntakeCmd().asProxy();
		var swerveAim = swerve.aim(0).asProxy();
		var aimAndSpinUp = superstructure.aimAndSpinUp(() -> kPodiumAngle, false, true, true).until(superstructure.stateTrg_idle);
		var secondShot = superstructure.autonShootReq().asProxy();

		/* everything from 3 piece */
		var pathFollow2 = AutoBuilder.followPath(Paths.three).asProxy();
		var intake2 = superstructure.autonIntakeCmd().asProxy();
		var swerveAim2 = swerve.aim(0.413).asProxy();
		var aimAndSpinUp2 = superstructure.aimAndSpinUp(() -> kPodiumAngle, false, true, true);
		var thirdShot = superstructure.autonShootReq().asProxy();
		
		return Commands.sequence(
			/* 2 piece */
			Commands.print("running!!!!"),
			Commands.sequence(
				Commands.parallel(
					resetPose, 
					preloadShot),
				Commands.parallel(
					Commands.print("going to intake now"),
					intake,
					pathFollow
				),
				Commands.parallel(
					Commands.sequence(
						Commands.waitUntil(superstructure.stateTrg_noteReady),
						Commands.print("note ready")
					),
					Commands.sequence(
						swerveAim.withTimeout(0.1),
						Commands.print("aimed")
					)
				),
				Commands.print("note ready & aimed"),
				Commands.parallel(
					aimAndSpinUp,
					secondShot
				)
			),
			/* 3 piece */
			Commands.parallel(
				intake2,
				pathFollow2
			),
			Commands.parallel(
				swerveAim2.withTimeout(0.1),
				aimAndSpinUp2,
				thirdShot
			)
		);
	}

	public static Command fourPc(Superstructure superstructure, Shooter shooter, Swerve swerve) {
		var firstThree = threePc(superstructure, shooter, swerve);
		var pathFollow = AutoBuilder.followPath(Paths.four);
		var intake = superstructure.autonIntakeCmd().asProxy();
		var swerveAim = swerve.aim(0).asProxy();
		var aimAndSpinUp = superstructure.aimAndSpinUp(() -> kPodiumAngle, false, true, true);
		var fourthShot = superstructure.autonShootReq().asProxy();
		
		return Commands.sequence(
			firstThree,
			Commands.parallel(
				intake, // maybe wait a bit
				pathFollow
			),
			Commands.parallel(
				swerveAim,
				aimAndSpinUp,
				fourthShot
			)
		);
	}

	public static Command shoot(Superstructure superstructure, Shooter shooter) {
		var aimAndSpinUp = superstructure.aimAndSpinUp(() -> kSubwooferAngle, false, false, true).until(superstructure.stateTrg_idle);
		var waitUntilSpunUp = Commands.waitUntil(() -> shooter.spinUpFinished());
		var noteReady = superstructure.forceStateToNoteReady().asProxy();
		var shoot = Commands.runOnce(() -> superstructure.forceStateToShooting()).asProxy();

		return Commands.sequence(
			noteReady,
			Commands.parallel(
				Commands.print("aim and spin up"),
				aimAndSpinUp.andThen(Commands.print("aimAndSpinUp_DONE")),
				Commands.sequence(
					Commands.print("waiting for spin up"),
					waitUntilSpunUp.andThen(Commands.print("waitUntilSpunUp_DONE")),
					Commands.print("shooting"),
					shoot.andThen(Commands.print("shoot_DONE")),
					Commands.print("shot")
				)
			)
		);
	}

	private static Command incrementState() {
		return Commands.runOnce(
			() -> {
				currentState++;
				log_state.accept(
					currentState);
			}
		);
	}
}

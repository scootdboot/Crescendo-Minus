// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Inches;

import org.photonvision.PhotonCamera;

import com.choreo.lib.ChoreoTrajectory;
import com.ctre.phoenix6.SignalLogger;
import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModule.DriveRequestType;
import com.pathplanner.lib.commands.FollowPathCommand;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;
import frc.robot.Constants.AimK;
import frc.robot.Constants.FieldK;
import frc.robot.Constants.FieldK.SpeakerK;
import frc.robot.auton.AutonChooser;
import frc.robot.auton.AutonFactory;
import frc.robot.auton.AutonChooser.AutonOption;
import frc.robot.auton.Trajectories;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.shooter.Aim;
import frc.robot.subsystems.shooter.Conveyor;
import frc.robot.subsystems.shooter.Shooter;
import frc.robot.subsystems.shooter.Trap;
import frc.util.AllianceFlipUtil;
import frc.util.WaltRangeChecker;
import frc.util.logging.WaltLogger;
import frc.util.logging.WaltLogger.BooleanLogger;
import frc.util.logging.WaltLogger.DoubleLogger;
import frc.robot.subsystems.Climber;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Superstructure;

import static frc.robot.Constants.AimK.kAmpAngle;
import static frc.robot.Constants.AimK.kClimbAngle;
import static frc.robot.Constants.AimK.kClimbingAngle;
import static frc.robot.Constants.AimK.kSubwooferAngle;
import static frc.robot.Constants.AimK.kTrapAngle;
import static frc.robot.Constants.RobotK.*;

import java.util.function.Supplier;

public class Robot extends TimedRobot {
	/** 5.21 meters per second desired top speed */
	public static final double kMaxSpeed = 5;
	/** 1.5 of a rotation per second max angular velocity */
	public static final double kMaxAngularRate = 1.5 * (Math.PI * 2);

	/* Setting up bindings for necessary control of the swerve drive platform */
	private final CommandXboxController driver = new CommandXboxController(0); // My joystick
	private final CommandXboxController manipulator = new CommandXboxController(1);

	private final Swerve swerve = TunerConstants.drivetrain;
	/** Object of Vision class */
	private final Vision vision = new Vision();
	private final Shooter shooter = new Shooter();
	private final Aim aim = new Aim();
	private final Intake intake = new Intake();
	private final Conveyor conveyor = new Conveyor();
	private final Climber climber = new Climber();
	private final Trap trap = new Trap();

	private final PowerDistribution pdp = new PowerDistribution();
	private final DoubleLogger log_miniPcPower = WaltLogger.logDouble("MiniPc", "power");
	private final BooleanLogger log_powerAbove10 = WaltLogger.logBoolean("MiniPc", "powerGreaterThan10");
	private double miniPcPower;

	private final Trigger trapTrg = manipulator.start();

	public final Superstructure superstructure = new Superstructure(
		aim, intake, conveyor, shooter, vision,
		manipulator.leftTrigger(), driver.rightTrigger(), manipulator.leftBumper().and(driver.rightTrigger()), trapTrg.or(manipulator.a()),
		(intensity) -> driverRumble(intensity), (intensity) -> manipulatorRumble(intensity));

	public static final Field2d field2d = new Field2d();

	private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
		.withDeadband(kMaxSpeed * 0.1) // Add a 5% deadband
		.withDriveRequestType(DriveRequestType.OpenLoopVoltage);
	private final SwerveRequest.RobotCentric robotCentric = new SwerveRequest.RobotCentric()
		.withDriveRequestType(DriveRequestType.OpenLoopVoltage);
	private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
	private final Telemetry logger = new Telemetry(kMaxSpeed);

	private final BooleanLogger log_frontCamEstPresent = WaltLogger.logBoolean("Swerve", "frontCamEstPresent");

	private Command m_autonomousCommand;

	public Robot() {
		DriverStation.silenceJoystickConnectionWarning(true);
		PhotonCamera.setVersionCheckEnabled(false);
		// disable joystick not found warnings when in sim
		if (Robot.isSimulation()) {
			DriverStation.silenceJoystickConnectionWarning(true);
		}
		addPeriodic(() -> {
			var frontCamEstOpt = vision.getFrontCamPoseEst();
			boolean frontCamTagsPresent = frontCamEstOpt.hasTarget();
			boolean frontCamEstPresent = frontCamEstOpt.measOpt().isPresent();
			log_frontCamEstPresent.accept(frontCamEstPresent);
			swerve.calculateYawErr(frontCamEstOpt.measOpt(), frontCamTagsPresent);
			if (frontCamEstPresent) {
				var frontEst = frontCamEstOpt.measOpt().get();
				aim.calculatePitchToSpeaker(frontEst);
				// swerve.addVisionMeasurement(frontEst.estimate().estimatedPose.toPose2d(), frontEst.estimate().timestampSeconds);
			};
		}, 0.02);
		miniPcPower = pdp.getCurrent(17) * pdp.getVoltage();
		WaltRangeChecker.addDoubleChecker("MiniPc", () -> miniPcPower, 10, 70, 1, false);
	}

	private void mapAutonCommands() {
		AutonChooser.setDefaultAuton(AutonOption.DO_NOTHING);
		AutonChooser.assignAutonCommand(AutonOption.DO_NOTHING, Commands.none());
		AutonChooser.assignAutonCommand(AutonOption.PRELOAD, AutonFactory.one(superstructure, shooter, aim));
		AutonChooser.assignAutonCommand(AutonOption.AMP_TWO, AutonFactory.ampTwo(superstructure, shooter, swerve, aim),
			Trajectories.ampSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.AMP_THREE, AutonFactory.ampThree(superstructure, shooter, swerve, aim), 
			Trajectories.ampSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.AMP_FOUR, AutonFactory.ampFour(superstructure, shooter, swerve, aim), 
			Trajectories.ampSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.AMP_FIVE, AutonFactory.ampFive(superstructure, shooter, swerve, aim), 
			Trajectories.ampSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.SOURCE_TWO, AutonFactory.sourceTwo(superstructure, shooter, swerve, aim),
			Trajectories.sourceSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.SOURCE_THREE, AutonFactory.sourceThree(superstructure, shooter, swerve, aim),
			Trajectories.sourceSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.SOURCE_THREE_POINT_FIVE, AutonFactory.sourceThreePointFive(superstructure, shooter, swerve, aim),
			Trajectories.sourceSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.SOURCE_FOUR, AutonFactory.sourceFour(superstructure, shooter, swerve, aim),
			Trajectories.sourceSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.VERY_AMP_THREE_POINT_FIVE, AutonFactory.veryAmpThreePointFive(superstructure, shooter, swerve, aim),
			Trajectories.sourceSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.G28_COUNTER, AutonFactory.g28Counter(superstructure, shooter, swerve, aim),
			Trajectories.g28Counter.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.SILLY_AMP_FIVE, AutonFactory.sillyFive(superstructure, shooter, swerve, aim),
			Trajectories.ampSide.getInitialPose());
		AutonChooser.assignAutonCommand(AutonOption.MADTOWN, AutonFactory.madtown(superstructure, shooter, swerve, aim),
			Trajectories.sourceSide.getInitialPose());
	}

	private void driverRumble(double intensity) {
		if (!DriverStation.isAutonomous()) {
			driver.getHID().setRumble(RumbleType.kBothRumble, intensity);
		}
	}

	private void manipulatorRumble(double intensity) {
		if (!DriverStation.isAutonomous()) {
			manipulator.getHID().setRumble(RumbleType.kBothRumble, intensity);
		}
	}

	private Supplier<SwerveRequest.FieldCentric> getTeleSwerveReq() {
		return () -> {
			double leftY = -driver.getLeftY();
			double leftX = -driver.getLeftX();
			return drive
				.withVelocityX(leftY * kMaxSpeed)
				.withVelocityY(leftX * kMaxSpeed)
				.withRotationalRate(-driver.getRightX() * kMaxAngularRate)
				.withRotationalDeadband(kMaxAngularRate * 0.1);
		};
	}

	private void configureBindings() {
		/* drivetrain */
		if (Utils.isSimulation()) {
			swerve.seedFieldRelative(new Pose2d(new Translation2d(), Rotation2d.fromDegrees(90)));
		}
		swerve.registerTelemetry(logger::telemeterize);

		/* driver controls */
		swerve.setDefaultCommand(swerve.applyFcRequest(getTeleSwerveReq()));

		// swerve brake
		driver.a().whileTrue(swerve.applyRequest(() -> brake));

		// force shot
		driver.b().and(driver.rightTrigger()).onTrue(superstructure.forceStateToShooting());

		// rezero
		driver.leftBumper().onTrue(swerve.runOnce(() -> swerve.seedFieldRelative()));

		// centre of gravity while climbing
		driver.y().onTrue(aim.toAngleUntilAt(kClimbingAngle));

		// face amp
		driver.leftTrigger().whileTrue(swerve.faceAmp(() -> -driver.getLeftY(), () -> -driver.getLeftX(), kMaxSpeed));
		driver.start().whileTrue(swerve.faceAmpUnderDefence(() -> -driver.getLeftY(), () -> -driver.getLeftX(), kMaxSpeed));

		/* manipulator controls */
		// eject note
		manipulator.rightTrigger().whileTrue(intake.outtake());

		manipulator.x().and((manipulator.rightBumper().or(manipulator.povUp())).negate()).onTrue(aim.hardStop());

		// subwoofer shot prep
		manipulator.rightBumper().whileTrue(shooter.subwoofer());

		// amp shot prep
		manipulator.leftBumper().and(manipulator.a().negate()).whileTrue(superstructure.ampShot(kAmpAngle));

		// manip force shot
		manipulator.b().and(manipulator.povUp())
			.onTrue(superstructure.forceStateToShooting());

		// manip force FSM to intake
		manipulator.b().and(manipulator.leftTrigger()).onTrue(superstructure.forceStateToIntake());

		// aim safe angle
		// manipulator.x().and(manipulator.rightBumper().negate()).and(manipulator.a().negate()).onTrue(aim.hardStop());

		// subwoofer if vision breaky
		manipulator.x().and(manipulator.rightBumper()).onTrue(aim.toAngleUntilAt(kSubwooferAngle));

		// aim rezero
		manipulator.b().and(manipulator.povDown()).and(manipulator.x()).onTrue(aim.rezero());

		// aim amp
		manipulator.leftBumper().and(manipulator.y()).onTrue(aim.toAngleUntilAt(() -> AimK.kAmpAngle, Degrees.of(0.25)));

		// testing buttons. COMMENT OUT WHEN DONE !!!!! (watch me forget. sorry future grac. and everyone else involved.)
		// also uncomment out first two climber controls.
		// aim slowly go down
		// manipulator.a().and(manipulator.povDown()).onTrue(aim.decreaseAngle());
		// manipulator.a().and(manipulator.povUp()).whileTrue(aim.increaseAngle());

		// climber controls	
		// x is override button
		manipulator.a().and(manipulator.povDown()).whileTrue(climber.retractBoth());
		manipulator.a().and(manipulator.povUp()).whileTrue(climber.extendBoth());
		manipulator.a().and(manipulator.povLeft()).whileTrue(climber.retractLeft());
		manipulator.a().and(manipulator.povRight()).whileTrue(climber.retractRight());
		manipulator.y().and(manipulator.leftBumper().negate()).onTrue(aim.toAngleUntilAt(kClimbAngle));

		// trap buttons
		trapTrg.whileTrue(shooter.trap())
			.onTrue(aim.toAngleUntilAt(kTrapAngle))
			.onTrue(trap.deploy())
			.onFalse(trap.stop());

		// feeding
		manipulator.povUp().and((manipulator.a().or(manipulator.b())).negate()).whileTrue(shooter.lob())
			.and(manipulator.x()).onTrue(aim.toAngleUntilAt(Degrees.of(15.5)));

		manipulator.povLeft().and(manipulator.a().negate()).onTrue(aim.toAngleUntilAt(Degrees.of(15.5))); // TODO unmagify

		driver.rightBumper().whileTrue(shooter.farShot());
	}

	public void configureTestingBindings() {
		// spinny buttons
		driver.back().and(driver.x()).whileTrue(swerve.sysIdDynamic(Direction.kForward));
		driver.back().and(driver.y()).whileTrue(swerve.sysIdDynamic(Direction.kReverse));
		driver.start().and(driver.x()).whileTrue(swerve.sysIdQuasistatic(Direction.kForward));
		driver.start().and(driver.y()).whileTrue(swerve.sysIdQuasistatic(Direction.kReverse));

		// sysid buttons
		// manipulator.back().and(manipulator.x()).whileTrue(shooter.sysIdDynamic(Direction.kForward));
		// manipulator.back().and(manipulator.y()).whileTrue(shooter.sysIdDynamic(Direction.kReverse));
		// manipulator.start().and(manipulator.x()).whileTrue(shooter.sysIdQuasistatic(Direction.kForward));
		// manipulator.start().and(manipulator.y()).whileTrue(shooter.sysIdQuasistatic(Direction.kReverse));

		driver.povUp().onTrue(shooter.increaseRpm());
		driver.povDown().onTrue(shooter.decreaseRpm());

		// manipulator.start().onTrue(superstructure.forceStateToNoteReady());

		// wheel pointy straight for pit
		driver.povUp().and(driver.start())
			.whileTrue(swerve.applyRequest(() -> robotCentric.withVelocityX(0.5)))
			.onTrue(swerve.resetModulePositions())
			.onTrue(Commands.runOnce(() -> swerve.seedFieldRelative(new Pose2d())));

		driver.back().onTrue(swerve.resetPoseToSpeaker());
	}

	private Command getAutonomousCommand() {
		return AutonChooser.getChosenAutonCmd();
	}

	@Override
	public void robotInit() {
		addPeriodic(() -> {
			superstructure.fastPeriodic();
		}, 0.00125);
		SmartDashboard.putData(field2d);
		WaltLogger.logPose3d("FieldPoses", "shotLocation").accept(
			Vision.getMiddleSpeakerTagPose().transformBy(AimK.kTagToSpeaker));
		WaltLogger.logPose3d("FieldPoses", "tag4Location")
			.accept(FieldK.kTag4Pose);
		WaltLogger.logPose3d("FieldPoses", "tag7Location")
			.accept(FieldK.kTag7Pose);
		mapAutonCommands();
		configureBindings();
		DriverStation.startDataLog(DataLogManager.getLog());
		if (!DriverStation.isFMSAttached()) {
			configureTestingBindings();
		}
		if (kTestMode) {
			swerve.setTestMode();
		}
		FollowPathCommand.warmupCommand();
	}

	@Override
	public void robotPeriodic() {
		CommandScheduler.getInstance().run();
		swerve.logModulePositions();
		miniPcPower = pdp.getCurrent(17) * pdp.getVoltage();
		log_miniPcPower.accept(miniPcPower);
		log_powerAbove10.accept(miniPcPower > 10);
	}

	@Override
	public void disabledInit() {
		SignalLogger.stop();
	}

	@Override
	public void disabledPeriodic() {
	}

	@Override
	public void disabledExit() {
	}

	@Override
	public void autonomousInit() {
		SignalLogger.start();
		m_autonomousCommand = getAutonomousCommand();
		if (m_autonomousCommand != null) {
			m_autonomousCommand.schedule();
		}
		superstructure.m_autonTimer.restart();
		superstructure.shotNumber = 0;
	}

	@Override
	public void autonomousPeriodic() {
	}

	@Override
	public void autonomousExit() {
	}

	@Override
	public void teleopInit() {
		SignalLogger.start();

		if (m_autonomousCommand != null) {
			m_autonomousCommand.cancel();
		}

		superstructure.resetAutonFlags();
	}

	@Override
	public void teleopPeriodic() {
	}

	@Override
	public void teleopExit() {
	}

	@Override
	public void testInit() {
		CommandScheduler.getInstance().cancelAll();
	}

	@Override
	public void testPeriodic() {
	}

	@Override
	public void testExit() {
	}

	@Override
	public void simulationPeriodic() {
		getTrajLines();
		simulateAim();
	}

	private void getTrajLines() {
		var traj = AutonChooser.getChosenTrajectory();
		var alliance = DriverStation.getAlliance();
		ChoreoTrajectory realTraj;
		if (alliance.isPresent() && alliance.get().equals(Alliance.Red)) {
			realTraj = traj.flipped();
		} else {
			realTraj = traj;
		}
		field2d.getObject("trajectory").setPoses(realTraj.getPoses());
	}

	private void simulateAim() {
		var drivePose = swerve.getState().Pose;
		var y = Inches.of(Math.sin(drivePose.getRotation().getRadians()));
		var speakerPose2d = AllianceFlipUtil.apply(SpeakerK.kBlueCenterOpeningPose3d.toPose2d());
		var endPose = drivePose
			.plus(new Transform2d(new Translation2d(AimK.kLength.negate(), y), new Rotation2d()));
		field2d.getObject("desAimPose").setPoses(drivePose, speakerPose2d);
		field2d.getObject("aimPose").setPoses(drivePose, endPose);
		field2d.getRobotObject().setPose(drivePose);
	}
}

package frc.robot;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Minute;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static frc.robot.Robot.kMaxSpeed;

import java.util.List;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.ClosedLoopRampsConfigs;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.configs.TorqueCurrentConfigs;
import com.ctre.phoenix6.signals.AbsoluteSensorRangeValue;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.units.Angle;
import edu.wpi.first.units.Distance;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Velocity;
import frc.robot.generated.TunerConstants;
import frc.util.AllianceFlipUtil;

public class Constants {
    public static boolean kDebugLoggingEnabled = true;
    public static final double kMetersPerInch = 0.0254;

    public static final double kStickDeadband = 0.1;
    public static final String kCanbus = "fd";

    public static final Velocity<Angle> RotationsPerMinute = Rotations.per(Minute);

    public class VisionK {
        public static final Transform3d kFrontTagCamLocation = new Transform3d(
            0.5, 0.5, 0.25, new Rotation3d());

        public static final Transform3d kRearTagCamLocation = new Transform3d(
            -0.5, 0.5, 0.25, new Rotation3d(0, 0, Units.degreesToRadians(180)));
    }

    public static final class DriveK {
        public static final double kDutyCycleOpenLoopRamp = 0.05; // seconds
    }

    public class FieldK {
        public static final Measure<Distance> kFieldLength = Meters.of(16.54);
        public static final Measure<Distance> kFieldWidth = Meters.of(8.21);

        /** 
         * @param pose
         * @return Returns whether or not the given Pose3d is within the bounds of the field
         */
        public static boolean inField(Pose3d pose) {
            return (pose.getX() > 0
                && pose.getX() < kFieldLength.in(Meters)
                && pose.getY() > 0
                && pose.getY() < kFieldWidth.in(Meters));
          }

        public static final AprilTagFieldLayout kTagLayout = AprilTagFields.k2024Crescendo.loadAprilTagLayoutField();
        private static final List<AprilTag> kSpeakerTags =
            List.of(kTagLayout.getTags().get(2), kTagLayout.getTags().get(3), kTagLayout.getTags().get(6), kTagLayout.getTags().get(7));
        public static final AprilTagFieldLayout kTagLayout_SpeakerOnly = new AprilTagFieldLayout(
            kSpeakerTags, kTagLayout.getFieldLength(), kTagLayout.getFieldWidth());
        public static final Pose3d kTag4Pose = kTagLayout.getTagPose(4).get();
        public static final Pose3d kTag7Pose = kTagLayout.getTagPose(7).get();

        // taken from 6328. All in blue alliance origin.
        /* speaker constants */
        public static final class SpeakerK {
            private static final Measure<Distance> kTopX = Inches.of(18.055);
            private static final Measure<Distance> kTopZ = Inches.of(83.091);
            public static final Translation3d kTopRight = new Translation3d(
                kTopX, Inches.of(238.815), kTopZ);
            public static final Translation3d kTopLeft = new Translation3d(
                kTopX, Inches.of(197.765), kTopZ);

            private static final Measure<Distance> kBotX = Inches.of(0);
            private static final Measure<Distance> kBotZ = Inches.of(78.324);
            // private static final Translation3d kBotRight = new Translation3d(
            //     kBotX, Inches.of(238.815), kBotZ);
            public static final Translation3d kBotLeft = new Translation3d(
                kBotX, Inches.of(197.765), kBotZ);

            public static final Translation3d kBlueCenterOpening = kBotLeft.interpolate(kTopRight, 0.5);
            public static final Pose3d kBlueCenterOpeningPose3d = new Pose3d(
                kBlueCenterOpening, new Rotation3d());
            public static final Translation3d kRedCenterOpening = AllianceFlipUtil.flip(kBlueCenterOpening);
            public static final Pose3d kRedCenterOpeningPose3d = new Pose3d(
                kRedCenterOpening, new Rotation3d());

            public static final Measure<Distance> kAimOffset = Inches.of(25);
        }

        /* amp constants */
        public static final Measure<Distance> kXToAmp = Inches.of(49.5);
        public static final Measure<Distance> kYToAmp = Inches.of(286.765);
        public static final Measure<Distance> kZToAmp = Inches.of(35);

        public static final Translation3d kBlueAmpPose = new Translation3d(
            kXToAmp, kYToAmp, kZToAmp);

        public static final Translation3d kRedAmpPose = new Translation3d(
            kFieldLength.minus(kXToAmp), kFieldWidth.minus(kYToAmp), kZToAmp);

        /* stage constants */
        public static final double kBlueStageClearanceDs = Units.inchesToMeters(188.5);
        public static final double kBlueStageClearanceRight = Units.inchesToMeters(88.3);
        public static final double kBlueStageClearanceCenter = Units.inchesToMeters(243.2);
        public static final double kBlueStageClearanceLeft = Units.inchesToMeters(234.9);

        public static final double kRedStageClearanceDs = Units.inchesToMeters(542.2);
        public static final double kRedStageClearanceRight = Units.inchesToMeters(88.3);
        public static final double kRedStageClearanceCenter = Units.inchesToMeters(407.9);
        public static final double kRedStageClearanceLeft = Units.inchesToMeters(234.9);
    }

    public class AutoK {
        public static final double kPTranslation = 5;
        public static final double kPTheta = 7.5; // 1

        public static final HolonomicPathFollowerConfig kPathFollowerConfig = new HolonomicPathFollowerConfig(
            new PIDConstants(kPTranslation),
            new PIDConstants(kPTheta),
            kMaxSpeed,
            TunerConstants.kDriveRadius,
            new ReplanningConfig()
        );
    }

    public class IntakeK {
        public static final class IntakeConfigs {
            public static final TalonFXConfiguration kConfigs = new TalonFXConfiguration();

            static {
                kConfigs.CurrentLimits = kConfigs.CurrentLimits
                    .withStatorCurrentLimit(120)
                    .withStatorCurrentLimitEnable(true)
                    .withSupplyCurrentLimit(55)
                    .withSupplyCurrentLimitEnable(true);
            }
        }

        public static final int kIntakeId = 10;
        public static final int kFeederId = 11;
        public static final int kVisiSightId = 2;
    }

    public class ConveyorK {
        public static final String kDbTabName = "Conveyor";

        public static final int kConveyorId = 12;
    }

    public class ShooterK {
        public static final class ShooterConfigs {
            /* stuff that works for speaker */
            private static final double kPRightSpeaker = 0.2;
            private static final double kSRightSpeaker = 0.25884; // volts
            private static final double kVRightSpeaker = 0.063367;
            private static final double kARightSpeaker = 0.018238;

            private static final double kPLeftSpeaker = 0.2;
            private static final double kSLeftSpeaker = 0.2612; // volts
            private static final double kVLeftSpeaker = 0.061983;
            private static final double kALeftSpeaker = 0.019191;

            /* stuff that works for amp */
            private static final double kPRightAmp = 0.15;
            private static final double kSRightAmp = 0.11941; // volts
            private static final double kVRightAmp = 0.066954;
            private static final double kARightAmp = 0.025247;

            private static final double kPLeftAmp = 0.15;
            private static final double kSLeftAmp = 0.2612; // volts
            private static final double kVLeftAmp = 0.061983;
            private static final double kALeftAmp = 0.019191;

            public static final TalonFXConfiguration kRightConfigs = new TalonFXConfiguration();
            public static final TalonFXConfiguration kLeftConfigs = new TalonFXConfiguration();

            static {
                var currentLimits = new CurrentLimitsConfigs()
                    .withStatorCurrentLimitEnable(true)
                    .withStatorCurrentLimit(120)
                    .withSupplyCurrentLimitEnable(true)
                    .withSupplyCurrentLimit(75);
                var closedLoopRamps = new ClosedLoopRampsConfigs()
                    .withVoltageClosedLoopRampPeriod(0)
                    .withTorqueClosedLoopRampPeriod(0);
                var feedback = new FeedbackConfigs().withSensorToMechanismRatio(kGearRatio);
                var motorOutput = new MotorOutputConfigs().withNeutralMode(NeutralModeValue.Coast);
                var torqueCurrent = new TorqueCurrentConfigs()
                    .withPeakForwardTorqueCurrent(120)
                    .withPeakReverseTorqueCurrent(-120);

                kRightConfigs.Slot0 = kRightConfigs.Slot0
                    .withKP(kPRightSpeaker)
                    .withKS(kSRightSpeaker)
                    .withKV(kVRightSpeaker)
                    .withKA(kARightSpeaker);

                kRightConfigs.Slot1 = kRightConfigs.Slot1
                    .withKP(kPRightAmp)
                    .withKS(kSRightAmp)
                    .withKV(kVRightAmp)
                    .withKA(kARightAmp);

                kRightConfigs.Feedback = feedback;
                kRightConfigs.MotorOutput = motorOutput;
                kRightConfigs.ClosedLoopRamps = closedLoopRamps;
                kRightConfigs.TorqueCurrent = torqueCurrent;
                kRightConfigs.CurrentLimits = currentLimits;

                kLeftConfigs.Slot0 = kLeftConfigs.Slot0
                    .withKP(kPLeftSpeaker)
                    .withKS(kSLeftSpeaker)
                    .withKV(kVLeftSpeaker)
                    .withKA(kALeftSpeaker);

                kLeftConfigs.Slot1 = kLeftConfigs.Slot1
                    .withKP(kPLeftAmp)
                    .withKS(kSLeftAmp)
                    .withKV(kVLeftAmp)
                    .withKA(kALeftAmp);

                kLeftConfigs.Feedback = feedback;
                kLeftConfigs.MotorOutput = motorOutput;
                kLeftConfigs.ClosedLoopRamps = closedLoopRamps;
                kLeftConfigs.TorqueCurrent = torqueCurrent;
                kLeftConfigs.CurrentLimits = currentLimits;
            }
        }

        public static final String kDbTabName = "Shooter";

        public static final Measure<Velocity<Angle>> kAmpTolerance = RotationsPerSecond.of(1.25);
        public static final Measure<Velocity<Angle>> kBigShootTolerance = RotationsPerSecond.of(2);

        public static final int kRightId = 13;
        public static final int kLeftId = 14;

        public static final double kSpinAmt = 0.7;

        public static final double kSubwooferRpm = 7300;
        public static final double kLobRpm = 4500;
        public static final double kPodiumRpm = 7600;
        public static final double kAmpRpm = 750;
        public static final double kTrapRpm = 1400;

        public static final double kGearRatio = 18.0 / 36.0;

        public static final class FlywheelSimK {
            public static final double kMoi = 0.056699046875; // kg m^2

            // basically just how much faster the wheels have to spin for 1 meter more of
            // distance idk how this will work
            public static final double kRpmFactor = 100; // TODO fix this i just chose a random value
        }
    }

    public static class AimK {
        public static Transform3d kTagToSpeaker = new Transform3d(Units.inchesToMeters(10), 0.0, Units.inchesToMeters(24), new Rotation3d());
        public static final Transform3d kOriginToPivot = new Transform3d(Units.inchesToMeters(-5.24999996), Units.inchesToMeters(0), Units.inchesToMeters(7.5075), new Rotation3d());

        public static final class AimConfigs {
            private static final double kP = 200;
            private static final double kPVision = 40;
            private static final double kI = 0;
            private static final double kS = 0.9 * 1.25;
            private static final double kV = 37.44 / 1.25;
            private static final double kA = 2 / 1.25;
            public static final double kG = 0.16 * 1.25;

            public static final TalonFXConfiguration motorConfig = new TalonFXConfiguration();
            public static final CANcoderConfiguration cancoderConfig = new CANcoderConfiguration();

            static {
                motorConfig.Slot0 = motorConfig.Slot0
                    .withKP(kP)
                    .withKI(kI)
                    .withKS(kS)
                    .withKV(kV)
                    .withKA(kA);

                motorConfig.Slot1 = motorConfig.Slot1
                    .withKP(kPVision)
                    .withKI(kI)
                    .withKS(kS)
                    .withKV(kV)
                    .withKA(kA);

                motorConfig.Feedback = motorConfig.Feedback
                    .withFeedbackSensorSource(FeedbackSensorSourceValue.FusedCANcoder)
                    .withFeedbackRemoteSensorID(15)
                    .withRotorToSensorRatio(kGearRatio / 1.69) // ?
                    .withSensorToMechanismRatio(1.69 / 1.0);

                motorConfig.MotorOutput = motorConfig.MotorOutput
                    .withNeutralMode(NeutralModeValue.Brake)
                    .withPeakForwardDutyCycle(1)
                    .withPeakReverseDutyCycle(-1)
                    .withInverted(InvertedValue.Clockwise_Positive);

                motorConfig.CurrentLimits = motorConfig.CurrentLimits
                    .withStatorCurrentLimit(45)
                    .withStatorCurrentLimitEnable(true)
                    .withSupplyCurrentLimit(16)
                    .withSupplyCurrentLimitEnable(true);

                motorConfig.HardwareLimitSwitch = motorConfig.HardwareLimitSwitch
                    .withForwardLimitEnable(false)
                    .withReverseLimitEnable(false);

                motorConfig.SoftwareLimitSwitch = motorConfig.SoftwareLimitSwitch
                    .withForwardSoftLimitThreshold(0.27)
                    .withReverseSoftLimitThreshold(0)
                    .withForwardSoftLimitEnable(true)
                    .withReverseSoftLimitEnable(true);

                cancoderConfig.MagnetSensor = cancoderConfig.MagnetSensor
                    .withMagnetOffset(-0.385986328125)
                    .withSensorDirection(SensorDirectionValue.Clockwise_Positive)
                    .withAbsoluteSensorRange(AbsoluteSensorRangeValue.Signed_PlusMinusHalf);
            }

            public static final int kCoastSwitchId = 3;
        }

        public static final String kDbTabName = "Aim";

        public static final int kAimId = 20;
        public static final int kHomeSwitch = 1;

        // 5 * 5 * 4 : 1 gearbox, 24:61 belt drive, 254.167 : 1 total
        // omg! poofs number!
        public static final double kGearRatio = ((5.0 * 5.0 * 4.0) * (61.0 / 24.0));

        public static final Measure<Distance> kLength = Inches.of(18);
        // asin((22 - kHeightTilShooter) / kLength)
        public static final Measure<Angle> kStageClearance = Degrees.of(47.097);
        public static final Measure<Angle> kMinAngle = Rotations.of(0);
        public static final Measure<Angle> kInitAngle = Degrees.of(90);
        public static final Measure<Angle> kMaxAngle = Rotations.of(0.45);

        public static final Measure<Angle> kSubwooferAngle = Rotations.of(0.08);
        public static final Measure<Angle> kAmpAngle = Rotations.of(0.195);
        public static final Measure<Angle> kPodiumAngle = Degrees.of(6.5);
        public static final Measure<Angle> kTrapAngle = Degrees.of(74.2);
        public static final Measure<Angle> kClimbAngle = Degrees.of(60);
        public static final Measure<Angle> kClimbingAngle = Degrees.of(50);

        public static final Measure<Angle> kTolerance = Degrees.of(2);
        public static final double kPreloadTolerance = 0.25;
    }

    public class ClimberK {
        public static final int kLeftId = 32;
        public static final int kRightId = 31;

        public static final double kMaxExtensionPos = -277;
        public static final double kExtendDutyCycle = -1;
        public static final double kRetractDutyCycle = 1;
        public static final double kRetractSingleDutyCycle = 0.5;
    }

    public class RobotK {
        public static final String kDbTabName = "Superstructure";
        public static final Measure<Distance> kHeightTilShooter = Inches.of(7.533);
        public static final boolean kTestMode = false;
        public static final double kSimInterval = 0.020;

        public static final boolean kStopCoast = false;
    }
}

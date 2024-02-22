package frc.robot.subsystems.shooter;

import static frc.robot.Constants.ConveyorK.*;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkLowLevel.MotorType;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class Conveyor extends SubsystemBase {
    private final CANSparkMax m_conveyor = new CANSparkMax(kConveyorId, MotorType.kBrushless);
    private final DigitalInput m_beamBreak = new DigitalInput(0);

    private final Trigger m_note = new Trigger(() -> m_beamBreak.get()).negate();

    public Conveyor() {
        m_conveyor.setInverted(true);
    }

    public Command run(boolean ignoreSensor) {
        var go = runEnd(() -> {
            m_conveyor.set(0.35);
        }, () -> {
            m_conveyor.set(0);
        });

        if (!ignoreSensor) {
            go = go.until(m_note);
        }

        return go;
    }

    public void periodic() {
        SmartDashboard.putBoolean("beamBreak", m_beamBreak.get());
    }
}

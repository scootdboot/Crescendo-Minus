package frc.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;

public final class GeometryUtil {
    public enum Plane {
        XYPlane,
        XZPlane,
        YZPlane
    };

    public static Pose2d pose2dOnPlane(Pose3d pose3d, Plane plane) {
        switch(plane) {
            case XYPlane: return new Pose2d(pose3d.getX(), pose3d.getY(), pose3d.getRotation().toRotation2d());
            case XZPlane: return new Pose2d(pose3d.getX(), pose3d.getZ(), Rotation2d.fromRadians(pose3d.getRotation().getY()));
            case YZPlane: return new Pose2d(pose3d.getY(), pose3d.getZ(), Rotation2d.fromRadians(pose3d.getRotation().getX()));
            default: return new Pose2d();
        }
    }
}
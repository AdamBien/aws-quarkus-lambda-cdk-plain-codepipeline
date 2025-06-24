package airhacks.codebuild.control;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.CloudWatchLoggingOptions;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.IBuildImage;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.LoggingOptions;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

public interface MavenCodeBuild {
        // aws codebuild list-curated-environment-images
        IBuildImage BUILD_IMAGE = LinuxBuildImage.STANDARD_7_0;
        ComputeType COMPUTE_TYPE = ComputeType.SMALL;
        BuildSpec BUILD_SPEC = BuildSpec.fromSourceFilename("buildspec.yml");
        BuildSpec ST_SPEC_NAME = BuildSpec.fromSourceFilename("stspec.yml");

        public static PipelineProject createBuildProject(Construct scope, IBucket bucket, String projectName) {
                var pipelineProject = PipelineProject.Builder.create(scope, projectName + "PipelineProject")
                                .cache(Cache.none())
                                .buildSpec(BUILD_SPEC)
                                .projectName(projectName)
                                .logging(getBuildLoggingOptions(scope, projectName))
                                .environment(BuildEnvironment.builder()
                                                .computeType(COMPUTE_TYPE)
                                                .buildImage(BUILD_IMAGE)
                                                .build())
                                .build();
                var pipelineRole = pipelineProject.getRole();
                var policyStatement = allowAssumingCDKRole();
                pipelineRole.addToPrincipalPolicy(policyStatement);
                bucket.grantReadWrite(pipelineRole);
                CfnOutput.Builder.create(scope, "RoleARN").value(pipelineRole.getRoleArn()).build();
                return pipelineProject;
        }

        public static PipelineProject createSystemTestProject(Construct scope, String projectName) {
                var pipelineProject = PipelineProject.Builder.create(scope, projectName + "PipelineProject")
                                .cache(Cache.none())
                                .projectName(projectName)
                                .buildSpec(ST_SPEC_NAME)
                                .logging(getSystemTestLoggingOptions(scope, projectName))
                                .environment(BuildEnvironment.builder()
                                                .computeType(COMPUTE_TYPE)
                                                .buildImage(BUILD_IMAGE)
                                                .build())
                                .build();
                return pipelineProject;
        }

        static PolicyStatement allowAssumingCDKRole() {
                return PolicyStatement.Builder.create()
                                .effect(Effect.ALLOW)
                                .actions(List.of("sts:AssumeRole", "iam:PassRole"))
                                .resources(List.of("arn:aws:iam::*:role/cdk-*"))
                                .build();
        }

        static LoggingOptions getBuildLoggingOptions(Construct scope, String projectName) {
                var logGroup = LogGroup.Builder.create(scope, "CodbuildLogGroup")
                                .logGroupName("/codebuild/" + projectName)
                                .retention(RetentionDays.FIVE_DAYS).build();
                return LoggingOptions.builder()
                                .cloudWatch(CloudWatchLoggingOptions.builder()
                                                .logGroup(logGroup)
                                                .enabled(true)
                                                .build())
                                .build();
        }

        static LoggingOptions getSystemTestLoggingOptions(Construct scope, String projectName) {
                var logGroup = LogGroup.Builder.create(scope, "CodebuildLogGroupST")
                                .logGroupName("/codebuild/" + projectName)
                                .retention(RetentionDays.FIVE_DAYS).build();
                return LoggingOptions.builder()
                                .cloudWatch(CloudWatchLoggingOptions.builder()
                                                .logGroup(logGroup)
                                                .enabled(true)
                                                .build())
                                .build();
        }

}

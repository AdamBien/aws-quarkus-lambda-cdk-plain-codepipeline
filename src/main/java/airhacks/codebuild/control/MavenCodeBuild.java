package airhacks.codebuild.control;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.CloudWatchLoggingOptions;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.IBuildImage;
import software.amazon.awscdk.services.codebuild.LinuxArmBuildImage;
import software.amazon.awscdk.services.codebuild.LoggingOptions;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

public interface MavenCodeBuild{
    // aws codebuild list-curated-environment-images
    static IBuildImage BUILD_IMAGE = LinuxArmBuildImage.fromCodeBuildImageId("aws/codebuild/amazonlinux2-aarch64-standard:3.0-24.10.29");    
    static ComputeType COMPUTE_TYPE = ComputeType.SMALL;


    public static PipelineProject createPipelineProject(Construct scope, IBucket bucket,String projectName) {
        var pipelineProject = PipelineProject.Builder.create(scope, projectName + "PipelineProject")
                .cache(Cache.none())
                .projectName(projectName)
                .logging(getLoggingOptions(scope,projectName))
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

    static PolicyStatement allowAssumingCDKRole() {
        return PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("sts:AssumeRole", "iam:PassRole"))
                .resources(List.of("arn:aws:iam::*:role/cdk-*"))
                .build();
    }

    static LoggingOptions getLoggingOptions(Construct scope,String projectName) {
        var logGroup = LogGroup.Builder.create(scope, "CodbuildLogGroup").logGroupName("/codebuild/" + projectName)
                .retention(RetentionDays.FIVE_DAYS).build();
        return LoggingOptions.builder()
                .cloudWatch(CloudWatchLoggingOptions.builder()
                        .logGroup(logGroup)
                        .enabled(true)
                        .build())
                .build();
    }


}

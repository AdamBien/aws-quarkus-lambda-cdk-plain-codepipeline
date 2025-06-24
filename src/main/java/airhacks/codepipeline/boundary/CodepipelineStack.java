package airhacks.codepipeline.boundary;

import java.util.List;

import airhacks.codebuild.control.MavenCodeBuild;
import airhacks.s3.control.ArtifactBucket;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.codebuild.IProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.IAction;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionType;
import software.amazon.awscdk.services.codepipeline.actions.CodeStarConnectionsSourceAction;
import software.constructs.Construct;

public class CodepipelineStack extends Stack {
        static Artifact SOURCE_OUTPUT = Artifact.artifact("source");

        public CodepipelineStack(Construct scope, GithubConfiguration configuration, String projectName,
                        String codestarConnectionARN) {
                super(scope, projectName + "-codepipeline");
                var systemTestProjectName = projectName + "-st";
                var artifactBucket = ArtifactBucket.create(this);
                var pipeline = Pipeline.Builder.create(this, projectName + "Pipeline")
                                .crossAccountKeys(true)
                                .artifactBucket(artifactBucket)
                                .pipelineName(projectName)
                                .build();
                pipeline.addStage(createStage("github-checkout",
                                List.of(createGithubConnection(codestarConnectionARN, configuration))));
                var buildProject = MavenCodeBuild.createBuildProject(this, artifactBucket, projectName);
                var testProject = MavenCodeBuild.createSystemTestProject(this, systemTestProjectName);

                var actions = List.of(
                                createCodeBuildAction("build", buildProject),
                                createCodeBuildAction("system-test", testProject));
                pipeline.addStage(createStage("build-and-deploy", actions));
                CfnOutput.Builder.create(this, "PipelineOutput").value(pipeline.getPipelineArn()).build();
        }

        StageOptions createStage(String stageName, List<? extends IAction> actions) {
                return StageOptions.builder()
                                .stageName(stageName)
                                .actions(actions)
                                .build();
        }

        CodeStarConnectionsSourceAction createGithubConnection(String codestarConnectionARN,
                        GithubConfiguration configuration) {
                return CodeStarConnectionsSourceAction.Builder.create()
                                .actionName("checkout-from-github")
                                .branch(configuration.branch())
                                .repo(configuration.repository())
                                .owner(configuration.owner())
                                .output(SOURCE_OUTPUT)
                                .connectionArn(codestarConnectionARN)
                                .build();
        }

        CodeBuildAction createCodeBuildAction(String actionName, IProject project) {
                return CodeBuildAction.Builder.create()
                                .project(project)
                                .actionName(actionName)
                                .input(SOURCE_OUTPUT)
                                .type(CodeBuildActionType.BUILD)
                                .build();
        }
}

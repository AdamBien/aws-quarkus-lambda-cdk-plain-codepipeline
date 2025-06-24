package airhacks.codepipeline.boundary;

import java.util.List;
import java.util.Map;

import airhacks.codebuild.control.MavenCodeBuild;
import airhacks.s3.control.ArtifactBucket;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType;
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

                var build = createCodeBuildAction(1, "build", buildProject);
                var systemTest = createCodeBuildAction(2, "system-test", testProject,
                                Map.of("BASE_URI_MP_REST_URL", BuildEnvironmentVariable.builder()
                                                .type(BuildEnvironmentVariableType.PARAMETER_STORE)
                                                .value("%s/BASE_URI_MP_REST_URL".formatted(systemTestProjectName))
                                                .build()));
                var actions = List.of(build, systemTest);
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

        CodeBuildAction createCodeBuildAction(int runOrder, String actionName, IProject project,
                        Map<String, BuildEnvironmentVariable> configuration) {
                return CodeBuildAction.Builder.create()
                                .project(project)
                                .runOrder(runOrder)
                                .actionName(actionName)
                                .input(SOURCE_OUTPUT)
                                .environmentVariables(configuration)
                                .type(CodeBuildActionType.BUILD)
                                .build();
        }

        CodeBuildAction createCodeBuildAction(int runOrder, String actionName, IProject project) {
                return createCodeBuildAction(runOrder, actionName, project, Map.of());
        }
}

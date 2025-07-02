# Serverless CodePipeline

This project implements a serverless CI/CD pipeline using AWS CodePipeline, CodeBuild and AWS CodeStar. It automates the build, test, and deployment process for Java applications without requiring dedicated build servers.

Key features:
- Automated code builds triggered by Git repository changes
- Serverless architecture using AWS managed services
- Java application support with Maven build system
- Integration with AWS CodeStar connections for source control
- CloudFormation templates for infrastructure as code
- Automated release flow orchestration with Step Functions
- AWS Step Functions SDK integration integration with CloudWatch logs
- Event-driven architecture with EventBridge


## Setup

Create `configuration.json` file at the same level as `pom.xml` with the codestart connection ARN.

```json
{
    "codeStarConnectionARN": "arn:aws:codestar-connections:[YOUR_AWS_REGION]:[YOUR_AWS_ACCOUNT_ID]:connection/[CONNECTION_ID]"
}

```


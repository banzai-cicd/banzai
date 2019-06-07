#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiStepCfg
import com.ge.nola.BanzaiBaseStage

def call(BanzaiCfg cfg) {
  if (cfg.gitOps == null && cfg.deploy == null) { return }

  BanzaiBaseStage stage = new BanzaiBaseStage(
    pipeline: this,
    cfg: cfg,
    stageName: 'Deploy'
  )

  BanzaiStepCfg deployCfg
  stage.validate {
    if (cfg.gitOps) {
      if (!cfg.internal.gitOps.DEPLOY) {
        // if this is a GitOps repo then cfg.internal.gitOps.DEPLOY must be set
        return "${BRANCH_NAME} does qualify for GitOps deployment. Skipping ${stageName}"
      }

      deployCfg = new BanzaiStepCfg()
    } else {
      // see if this is a project repo with a deployment configuration
      deployCfg = findValueInRegexObject(cfg.deploy, BRANCH_NAME)
      
      if (deployCfg == null) {
        return "${BRANCH_NAME} does not match a 'deploy' branch pattern. Skipping ${stageName}"
      }
    }
  }

  stage.execute {
    String script = deployCfg.script ?: "deploy.sh"
    runScript(cfg, script, cfg.internal.gitOps.DEPLOY_ARGS)
  }
}

// def call(BanzaiCfg cfg) {
//   String stageName = 'Deploy'
//   BanzaiStepCfg deployCfg

//   if (cfg.gitOps) {
//     if (!cfg.internal.gitOps.DEPLOY) {
//       // if this is a GitOps repo then cfg.internal.gitOps.DEPLOY must be set
//       logger "${BRANCH_NAME} does qualify for GitOps deployment. Skipping ${stageName}"
//       return
//     }

//     deployCfg = new BanzaiStepCfg()
//   } else {
//     if (cfg.deploy == null) { return }

//     // see if this is a project repo with a deployment configuration
//     deployCfg = findValueInRegexObject(cfg.deploy, BRANCH_NAME)
    
//     if (deployCfg == null) {
//       logger "${BRANCH_NAME} does not match a 'deploy' branch pattern. Skipping ${stageName}"
//       return
//     }
//   } 

//   stage (stageName) {
//     try {
//       notify(cfg, [
//         scope: BanzaiEvent.Scope.STAGE,
//         status: BanzaiEvent.Status.PENDING,
//         stage: stageName,
//         message: 'Pending'
//       ])
//       // TODO: refactor deployArgs
//       String script = deployCfg.script ?: "deploy.sh"
//       runScript(cfg, script, cfg.internal.gitOps.DEPLOY_ARGS)
//       notify(cfg, [
//         scope: BanzaiEvent.Scope.STAGE,
//         status: BanzaiEvent.Status.SUCCESS,
//         stage: stageName,
//         message: 'Success'
//       ])
//     } catch (err) {
//       echo "Caught: ${err}"
//       currentBuild.result = 'FAILURE'
//       if (isGithubError(err)) {
//         notify(cfg, [
//           scope: BanzaiEvent.Scope.STAGE,
//           status: BanzaiEvent.Status.FAILURE,
//           stage: stageName,
//           message: 'githubdown'
//         ])
//       } else {
//         notify(cfg, [
//           scope: BanzaiEvent.Scope.STAGE,
//           status: BanzaiEvent.Status.FAILURE,
//           stage: stageName,
//           message: 'Failed'
//         ])   
//       }
      
//       error(err.message)
//     }
//   }

// }

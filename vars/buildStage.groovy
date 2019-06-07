#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiStepCfg
import com.ge.nola.BanzaiBaseStage

def call(BanzaiCfg cfg) {
  if (cfg.build == null) { return }

  BanzaiBaseStage stage = new BanzaiBaseStage(
    pipeline: this,
    cfg: cfg,
    stageName: 'Build'
  )

  BanzaiStepCfg buildCfg = findValueInRegexObject(cfg.build, BRANCH_NAME)
  stage.validate {
    if (buildCfg == null) {
      return "${BRANCH_NAME} does not match a 'build' branch pattern. Skipping ${stageName}"
    }
  }

  stage.execute {
    String script = buildCfg.script ?: "build.sh"
    runScript(cfg, script)
  }
}

// def call(BanzaiCfg cfg) {
//   if (cfg.build == null) { return } 

//   String stageName = 'Build'
//   BanzaiStepCfg buildCfg = findValueInRegexObject(cfg.build, BRANCH_NAME)

//   if (buildCfg == null) {
//     logger "${BRANCH_NAME} does not match a 'build' branch pattern. Skipping ${stageName}"
//     return
//   }

//   stage (stageName) {
//     try {
//       notify(cfg, [
//         scope: BanzaiEvent.Scope.STAGE,
//         status: BanzaiEvent.Status.PENDING,
//         stage: stageName,
//         message: 'Pending'
//       ])
//       String script = buildCfg.script ?: "build.sh"
//       runScript(cfg, script)
//       notify(cfg, [
//         scope: BanzaiEvent.Scope.STAGE,
//         status: BanzaiEvent.Status.SUCCESS,
//         stage: stageName,
//         message: 'Success'
//       ])      
//     } catch (err) {
//         echo "Caught: ${err}"
//         currentBuild.result = 'FAILURE'
//         if (isGithubError(err)) {
//           notify(cfg, [
//             scope: BanzaiEvent.Scope.STAGE,
//             status: BanzaiEvent.Status.FAILURE,
//             stage: stageName,
//             message: 'githubdown'
//           ])
//         } else {
//           notify(cfg, [
//             scope: BanzaiEvent.Scope.STAGE,
//             status: BanzaiEvent.Status.FAILURE,
//             stage: stageName,
//             message: 'Failed'
//           ])
//         }
        
//         error(err.message)
//     }
//   }

// }

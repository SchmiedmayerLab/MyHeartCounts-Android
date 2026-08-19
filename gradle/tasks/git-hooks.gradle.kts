//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

import org.gradle.api.tasks.Copy

/**
 * A gradle task that registers the git hooks from internal/git-hooks to the .git folder
 */
val installGitHooks by tasks.registering(Copy::class) {
    group = "grove"
    description = "Copies and installs the git hooks from internal/git-hooks to the .git folder."

    val gitHooksDir = file("$rootDir/.git/hooks")

    from("$rootDir/internal/git-hooks/") {
        include("**/*.sh")
        rename("(.*).sh", "$1")
    }

    into(gitHooksDir)

    doLast {
        gitHooksDir.listFiles()?.forEach { file ->
            file.setExecutable(true)
            logger.info("Git hook ${file.name} copied and installed successfully.")
        }
    }
}

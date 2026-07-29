plugins { id("org.danilopianini.gradle-pre-commit-git-hooks") }

gitHooks {
    commitMsg { conventionalCommits { defaultTypes() } }
    createHooks(true)
}

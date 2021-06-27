Nitwit
======

A GitHub App for managing Pull Requests to the Parchment mappings repository.

> # This project is a work-in-progress.
> Expect lots of changes, even scope-changing ones.

Development
-----------

Run the `generateApolloSources` task (directly, or through the `build` task) to generate the source files for the
GraphQL queries under `src/main/graphl`. Rerun this task whenever the schema or the query files are modified.

It is recommended to install the JS GraphQL plugin for IntelliJ IDEA. A bare-bones `.graphqlconfig` is provided in the
`src/main/graphql` directory which points to the schema file in `src/main/graphql/com/github/api/schema.json`.

To update the GitHub API schema file, run the following command with the `TOKEN` environment variable set to a valid
personal access token:

```shell
$ ./gradlew downloadApolloSchema \ 
    --endpoint="https://api.github.com/graphql" \
    --schema="src/main/graphql/schema.json" \
    --header="Authorization: Bearer $TOKEN"
```

License
=======

Copyright (c) 2021 ParchmentMC. Under the MIT License (see `LICENSE.txt`). 
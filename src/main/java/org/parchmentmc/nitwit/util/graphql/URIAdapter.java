package org.parchmentmc.nitwit.util.graphql;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class URIAdapter implements CustomTypeAdapter<URI> {
    public URIAdapter() {
    }

    @Override
    public URI decode(@NotNull CustomTypeValue<?> customTypeValue) {
        return URI.create(customTypeValue.value.toString());
    }

    @NotNull
    @Override
    public CustomTypeValue<?> encode(URI uri) {
        return new CustomTypeValue.GraphQLString(uri.toString());
    }
}

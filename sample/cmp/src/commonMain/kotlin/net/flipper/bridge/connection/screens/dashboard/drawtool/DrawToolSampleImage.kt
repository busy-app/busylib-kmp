package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal object DrawToolSampleImage {
    private const val BASE64_PNG =
        "iVBORw0KGgoAAAANSUhEUgAAAEgAAAAQCAIAAAA6Uq6KAAAEXUlEQVR42q3Sy0/UXByH8WeDF66iIogIKEIGQQbDRUEmlfFW" +
            "kEiHRLRBgwejwSaIqZhojlFDF2rSBBdNWDWs6rLLbvunvS+nDh0WNDMLWPzyPRnyyZPMoIFWh1aP1oLWhtaJ1oPWj3YdLY82" +
            "gTaNpjGtMaGR17iu0a/Ro9Gp0abRolGvUaeUY4NqUKpD6KDv3wb0VvQO9G70PvRB9FH0SfQZ9CK6TlFnRmdSZ1RnUKdPp1un" +
            "XadVp0GnTilZUP5oKJ8JpUp7Zk5WUUcKYYBxAqMJ4xxGJ8YVjBzGCMYExgzGPYw5DIM5g3sGMwYTBiMGOYMrBp0G5wyaDE4o" +
            "5digGpTqECaYJzGbMS9gXsbsxxzGHMO8g1nEnMcsYZqUTOZNiiZ3TMZMhk36TS6bXDBpNjmplGODalCqQwgQpxBnEB2IXkQO" +
            "MYq4jZhFzCFKCBMhMAUlwZxgVnBbMCrICXoFHYIzglNKyYLuHg3dzYRqyKlehAXW/m3F6sTqwxrGGscqYD3EWsR6jiWwLITF" +
            "c4tFi4cWBYtxi2GLPotOi1aLU0opQ/8d/P2DkqGgZCio8g8Ftf9BahxANeRUL8IG+zT2Wewu7AHsPPYUdhF7Afsp9ir2OrbN" +
            "us2qzVObBZuizZRN3qbfpsvmrM1ppZQhlZhAySgmV0HJsNU9BKlnAqlxAKU5/Zk5WUUDaRESZD3yPLIbOYgcQxaQj5BLyBfI" +
            "t8gPSMkHyVvJC8mS5JGkIBmT5CTdkvOSeqWUocoXkkDJUFAyFFT+tFSGys9DUJqTy8zJKhpMi3DA2b9tOL04QziTOLM4CzjP" +
            "cNZwNnA+4zh8dthwWHN45rDgMOsw6TDk0OvQ5lCvlDKkKhMoGQvJVVAyKpB6HkBqH4JqyKlehAtuA2477lXcEdxp3Ae4JdyX" +
            "uO9wt3B/4Lr8cNlyeefy0qXk8sBl2mXE5apLu0uDUsqQqkygZJSSq6BkVKDkSytDah+CasipXoQHXiNeB941vJt4Bbx5vGW8" +
            "13ibeBLvJ57HTw/psenx2mPZY96j4DHqcc2jw6NRKWWo8lMsQ2sHTwVVPl2rQOp5CEpzRjNzsopupkX44O/fi/gD+OP4s/hP" +
            "8Ffw1/E/4W/j7+D77Phs+3zyWfdZ8XniM+sz5jPgc9GnUSnHBqXKWKaSBY2nEAEETQSXCHIEtwjuEywRvCJ4T/CV4DfBLkHA" +
            "bsDvgK8B7wNeBSwF3A+YDMgFXApoUkoWtHE0tJEJpcpkZk5W0a0UIoSwmbCLcIhwmlAnXCZ8Q7hFuE34h3CPMGQv5E/IdshW" +
            "yJuQ5RA9ZCpkKKQrpFkpWdDHo6GPmVCqTGXmZBVNpxARRC1E3UQ3iGaIHhOtEFlEX4h+Ee0S/SWK+BuxG/Er4kuEFbES8Thi" +
            "JuJGRHdEi1KODapBqQ4RQ7x/e4jzxBrxIvEq8Sbxd+Id4j3ikDgmjNmL2Yn5HrMZsxqzGKPF5GN6YlqUkgV9Oxr6lgnVkFO9" +
            "6H8o9b/AF3ZIDgAAAABJRU5ErkJggg=="

    @OptIn(ExperimentalEncodingApi::class)
    fun bytes(): ByteArray = Base64.decode(BASE64_PNG)
}

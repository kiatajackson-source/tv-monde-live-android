package com.jackson.tvmonde

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONArray

@OptIn(UnstableApi::class)
class MainActivity : Activity() {
    private data class Channel(
        val id: String,
        val name: String,
        val country: String,
        val region: String,
        val category: String,
        val url: String,
        val description: String,
        val demo: Boolean
    )

    private val navy = Color.rgb(7, 19, 35)
    private val panel = Color.rgb(15, 32, 54)
    private val panel2 = Color.rgb(20, 42, 68)
    private val sky = Color.rgb(66, 197, 255)
    private val white = Color.rgb(243, 248, 255)
    private val muted = Color.rgb(164, 184, 205)
    private val green = Color.rgb(70, 220, 150)

    private val channels = mutableListOf<Channel>()
    private val favorites = mutableSetOf<String>()
    private val categories = listOf(
        "TOUT", "AFRIQUE", "EUROPE", "ASIE", "AMÉRIQUE",
        "ACTUALITÉS", "SPORT", "MUSIQUE", "ENFANTS", "FAVORIS"
    )

    private var activeCategory = "TOUT"
    private var searchText = ""
    private var currentChannel: Channel? = null
    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var playingTitle: TextView
    private lateinit var playbackStatus: TextView
    private lateinit var channelList: LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var categoryRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy

        val storedFavorites = getSharedPreferences("tv_monde_live", MODE_PRIVATE)
            .getStringSet("favorites", emptySet()) ?: emptySet()
        favorites.addAll(storedFavorites)
        loadChannels()
        buildInterface()
        initializePlayer()
        renderChannels()
    }

    private fun loadChannels() {
        try {
            val json = assets.open("channels.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val url = item.optString("url", "").trim()
                if (url.isBlank()) continue
                channels.add(
                    Channel(
                        id = item.optString("id", "channel-$i"),
                        name = item.optString("name", "Chaîne sans nom"),
                        country = item.optString("country", ""),
                        region = item.optString("region", "AUTRE").uppercase(),
                        category = item.optString("category", "AUTRE").uppercase(),
                        url = url,
                        description = item.optString("description", ""),
                        demo = item.optBoolean("demo", false)
                    )
                )
            }
        } catch (error: Exception) {
            Toast.makeText(this, "Catalogue impossible à lire : ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            setBackgroundColor(navy)
        }
        val logo = TextView(this).apply {
            text = "▶"
            textSize = 22f
            setTextColor(navy)
            gravity = Gravity.CENTER
            background = rounded(sky, sky, 16)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        val brandBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brandBox.addView(TextView(this).apply {
            text = "TV MONDE LIVE"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        })
        brandBox.addView(TextView(this).apply {
            text = "LA TÉLÉVISION DU MONDE, SUR UN ÉCRAN"
            textSize = 10f
            setTextColor(muted)
        })
        header.addView(logo)
        header.addView(brandBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            text = "HD\n1080"
            gravity = Gravity.CENTER
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(sky)
            background = rounded(panel2, sky, 12)
            setPadding(dp(10), dp(7), dp(10), dp(7))
        })
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(14))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(20))
        }

        body.addView(TextView(this).apply {
            text = "Regardez vos chaînes préférées"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        }, marginParams(bottom = dp(5)))
        body.addView(TextView(this).apply {
            text = "Gratuit • Sans compte • Lecteur intégré"
            textSize = 13f
            setTextColor(muted)
        }, marginParams(bottom = dp(14)))

        searchBox = EditText(this).apply {
            hint = "Rechercher une chaîne ou un pays…"
            textSize = 15f
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(panel, panel2, 12)
        }
        body.addView(searchBox, marginParams(bottom = dp(12)))
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchText = s?.toString()?.trim().orEmpty()
                renderChannels()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        categoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        categoryScroll.addView(categoryRow)
        body.addView(categoryScroll, marginParams(bottom = dp(14)))
        renderCategoryButtons()

        playerView = PlayerView(this).apply {
            useController = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setBackgroundColor(Color.BLACK)
            controllerAutoShow = true
        }
        body.addView(playerView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(220)
        ).apply { bottomMargin = dp(12) })

        playingTitle = TextView(this).apply {
            text = "Choisissez un flux dans le catalogue"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        }
        body.addView(playingTitle, marginParams(bottom = dp(5)))

        playbackStatus = TextView(this).apply {
            text = "Prêt • Le flux de démonstration sert uniquement à tester le lecteur."
            textSize = 12f
            setTextColor(green)
        }
        body.addView(playbackStatus, marginParams(bottom = dp(18)))

        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listHeader.addView(TextView(this).apply {
            text = "CATALOGUE"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(sky)
            letterSpacing = 0.12f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        listHeader.addView(TextView(this).apply {
            text = "Sélectionnez une chaîne"
            textSize = 11f
            setTextColor(muted)
        })
        body.addView(listHeader, marginParams(bottom = dp(10)))

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        body.addView(channelList)

        val footer = TextView(this).apply {
            text = "Important : le direct nécessite une connexion Internet. La qualité HD dépend du flux fourni, du réseau et de l'appareil."
            textSize = 11f
            setTextColor(muted)
            setPadding(dp(4), dp(18), dp(4), dp(6))
        }
        body.addView(footer)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun renderCategoryButtons() {
        categoryRow.removeAllViews()
        for (category in categories) {
            val selected = category == activeCategory
            val chip = TextView(this).apply {
                text = category
                textSize = 11f
                typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (selected) navy else white)
                setPadding(dp(13), dp(10), dp(13), dp(10))
                background = rounded(if (selected) sky else panel, if (selected) sky else panel2, 18)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    activeCategory = category
                    renderCategoryButtons()
                    renderChannels()
                }
            }
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38))
            params.rightMargin = dp(7)
            categoryRow.addView(chip, params)
        }
    }

    private fun renderChannels() {
        if (!::channelList.isInitialized) return
        channelList.removeAllViews()
        val matching = channels.filter { channel ->
            val categoryMatches = when (activeCategory) {
                "TOUT" -> true
                "FAVORIS" -> favorites.contains(channel.id)
                "AFRIQUE", "EUROPE", "ASIE", "AMÉRIQUE" -> channel.region.equals(activeCategory, ignoreCase = true)
                else -> channel.category.equals(activeCategory, ignoreCase = true)
            }
            val searchMatches = searchText.isBlank() ||
                channel.name.contains(searchText, ignoreCase = true) ||
                channel.country.contains(searchText, ignoreCase = true) ||
                channel.category.contains(searchText, ignoreCase = true)
            categoryMatches && searchMatches
        }

        if (matching.isEmpty()) {
            channelList.addView(TextView(this).apply {
                text = if (activeCategory == "FAVORIS") {
                    "Aucun favori pour le moment. Appuyez sur ☆ à côté d'une chaîne pour l'enregistrer."
                } else {
                    "Aucune chaîne dans cette rubrique pour le moment. Ajoutez des flux officiels autorisés dans app/src/main/assets/channels.json."
                }
                textSize = 13f
                setTextColor(muted)
                setPadding(dp(14), dp(18), dp(14), dp(18))
                background = rounded(panel, panel2, 12)
            })
            return
        }

        matching.forEach { channel ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(12), dp(10), dp(12))
                background = rounded(panel, panel2, 13)
                isClickable = true
                isFocusable = true
                setOnClickListener { playChannel(channel) }
            }
            val playIcon = TextView(this).apply {
                text = if (channel.demo) "▶" else "📺"
                textSize = 17f
                setTextColor(sky)
                gravity = Gravity.CENTER
                background = rounded(panel2, panel2, 10)
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            }
            val textBlock = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), 0, dp(6), 0)
            }
            textBlock.addView(TextView(this).apply {
                text = channel.name
                textSize = 14f
                maxLines = 2
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(white)
            })
            textBlock.addView(TextView(this).apply {
                text = listOf(channel.country, channel.category).filter { it.isNotBlank() }.joinToString(" • ")
                textSize = 11f
                setTextColor(muted)
                maxLines = 2
            }, marginParams(top = dp(3)))
            row.addView(playIcon)
            row.addView(textBlock, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val favoriteButton = TextView(this).apply {
                text = if (favorites.contains(channel.id)) "★" else "☆"
                textSize = 25f
                setTextColor(if (favorites.contains(channel.id)) sky else muted)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(4), dp(8), dp(4))
                isClickable = true
                setOnClickListener {
                    if (favorites.contains(channel.id)) favorites.remove(channel.id) else favorites.add(channel.id)
                    getSharedPreferences("tv_monde_live", MODE_PRIVATE).edit()
                        .putStringSet("favorites", favorites.toSet()).apply()
                    renderChannels()
                    renderCategoryButtons()
                }
            }
            row.addView(favoriteButton)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = dp(9)
            channelList.addView(row, params)
        }
    }

    private fun initializePlayer() {
        val selector = DefaultTrackSelector(this)
        selector.parameters = selector.buildUponParameters()
            .setMaxVideoSize(1920, 1080)
            .build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(selector)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val channel = currentChannel ?: return
                        when (playbackState) {
                            Player.STATE_BUFFERING -> playbackStatus.text = "Mise en mémoire tampon…"
                            Player.STATE_READY -> playbackStatus.text = if (channel.demo) {
                                "DÉMONSTRATION • Flux HLS de test, pas une chaîne TV"
                            } else {
                                "LECTURE • HD jusqu'à 1920 × 1080 si le flux la propose"
                            }
                            Player.STATE_ENDED -> playbackStatus.text = "Lecture terminée."
                            Player.STATE_IDLE -> Unit
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        playbackStatus.text = "Lecture impossible : vérifiez l'adresse du flux et votre connexion."
                        Toast.makeText(
                            this@MainActivity,
                            "Le flux ne peut pas être lu. Il peut être hors ligne ou bloqué.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
    }

    private fun playChannel(channel: Channel) {
        currentChannel = channel
        playingTitle.text = channel.name
        playbackStatus.text = if (channel.demo) {
            "DÉMONSTRATION • Ce flux sert à tester le lecteur, ce n'est pas une chaîne TV."
        } else {
            "Connexion au flux…"
        }
        try {
            player?.setMediaItem(MediaItem.fromUri(channel.url))
            player?.prepare()
            player?.playWhenReady = true
        } catch (error: Exception) {
            playbackStatus.text = "Impossible de démarrer ce flux."
            Toast.makeText(this, "Erreur de lecture : ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun rounded(fillColor: Int, strokeColor: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), strokeColor)
        }

    private fun marginParams(bottom: Int = 0, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = bottom
            topMargin = top
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}

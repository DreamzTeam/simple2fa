package io.dreamz.simple2fa.session.player

import io.dreamz.simple2fa.Simple2FA
import io.dreamz.simple2fa.session.Session
import io.dreamz.simple2fa.session.UserSession
import io.dreamz.simple2fa.settings.SessionSettings
import io.dreamz.simple2fa.storage.AsyncStorageEngine
import io.dreamz.simple2fa.utils.Base32String
import io.dreamz.simple2fa.utils.Time
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.Consumer

open class PlayerSession(private val uuid: UUID,
                         private val inventory: Array<ItemStack>,
                         private val armor: Array<ItemStack>,
                         private val location: Location) : UserSession {

    var authenticated = false
    var expireAt = -1L

    override fun expireAt(): Long = expireAt
    override fun isExpired(): Boolean = if (authenticated) System.currentTimeMillis() >= expireAt else false;

    override fun needsAuthentication(): Boolean = !authenticated
    override fun isAuthenticated(): Boolean = authenticated
    override fun getInventorySnapshot(): Array<out ItemStack>? = inventory
    override fun getArmorSnapshot(): Array<out ItemStack>? = armor
    override fun getLocationSnapshot(): Location = location
    override fun getPlayer(): Player = Bukkit.getPlayer(uuid)

    override fun authenticate(key: String, code: String?): Boolean {
        return this.verify(Base32String.decode(key), code!!)
    }

    private fun verify(key: ByteArray, code: String): Boolean {
        val totp = Simple2FA.instance.totp
        if (totp.verify(code, key, 2)) {
            this.authenticated = true
            this.expireAt = System.currentTimeMillis() + (Time.parseDuration(SessionSettings.expireAfter))
        }
        return this.authenticated
    }
}

class RemoteSession(private val authStatus: Boolean) : Session {
    override fun needsAuthentication(): Boolean {
        return !authStatus
    }

    override fun isAuthenticated(): Boolean {
        return authStatus
    }
}

object Sessions {
    @JvmStatic
    fun ofPlayer(player: Player) = PlayerSession(player.uniqueId, player.inventory.contents.clone(), player.inventory.armorContents.clone(), player.location.clone())

    @JvmStatic
    fun ofPlayerWithInfo(player: Player, expireAt: Long, authenticated: Boolean): Session {
        val session = ofPlayer(player)
        session.expireAt = expireAt
        session.authenticated = authenticated;
        return session
    }
}
package partyroom.versions;

import org.bukkit.Sound;

import partyroom.PartyRoom;

public class SoundHandler {
    
    public enum Sounds {
        ENTITY_ZOMBIE_ATTACK_IRON_DOOR("ZOMBIE_METAL"),
        BLOCK_NOTE_PLING("NOTE_PLING"),
        ENTITY_PLAYER_LEVELUP("LEVEL_UP"),
        ENTITY_CHICKEN_EGG("CHICKEN_EGG_POP"),
        ENTITY_ITEM_BREAK("ITEM_BREAK");
        
        private Sounds(String s) {
            sound = PartyRoom.VERSION.equals("v1_8_R3") ? Sound.valueOf(s.toUpperCase()) : Sound.valueOf(this.toString());
        }
        
        private Sound sound;
        
        public Sound a() {
            return sound;
        }
    }

}

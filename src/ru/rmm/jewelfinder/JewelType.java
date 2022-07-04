package ru.rmm.jewelfinder;

public enum JewelType {
    Elegant_Hubris("Elegant Hubris", new String[]{"explicit.pseudo_timeless_jewel_caspiro", "explicit.pseudo_timeless_jewel_victario", "explicit.pseudo_timeless_jewel_cadiro"}),
    Brutal_Restraint("Brutal Restraint", new String[]{"explicit.pseudo_timeless_jewel_asenath", "explicit.pseudo_timeless_jewel_balbala","explicit.pseudo_timeless_jewel_nasima"}),
    Lethal_Pride("Lethal Pride", new String[]{"explicit.pseudo_timeless_jewel_akoya", "explicit.pseudo_timeless_jewel_kaom", "explicit.pseudo_timeless_jewel_rakiata"}),
    Militant_Faith("Militant Faith", new String[]{"explicit.pseudo_timeless_jewel_dominus", "explicit.pseudo_timeless_jewel_maxarius", "explicit.pseudo_timeless_jewel_avarius"}),
    Glorious_Vanity("Glorious Vanity", new String[]{"explicit.pseudo_timeless_jewel_xibaqua", "explicit.pseudo_timeless_jewel_doryani", "explicit.pseudo_timeless_jewel_ahuana"});
    public String name;
    public String type = "Timeless Jewel";
    public String[] filters;
    JewelType(String name, String[] filters) {
        this.name = name;
        this.filters = filters;
    }
    public String toString(){ return name;}

}

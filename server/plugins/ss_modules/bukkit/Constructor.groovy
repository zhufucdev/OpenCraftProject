package bukkit

interface Constructor <T extends Constructor> {
    void apply()
    void unapply()
    void merge(T other)
}
package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

open class PageInventory<T: PageInventory.Adapter>: ClickableInventory {
    var adapter: T
    val itemsOnePage: Int
    val title: String
    final override val inventory: Inventory
    private val adapterCreator: (() -> T)?
    constructor(title: String, adapter: T, itemsOnePage: Int, plugin: Plugin): super(plugin){
        this.adapter = adapter
        adapterCreator = null
        this.itemsOnePage = itemsOnePage
        this.title = title
        inventory = Bukkit.createInventory(null,itemsOnePage,"$title[$id]")
        page(0)
    }
    constructor(title: String, adapter: () -> T, itemsOnePage: Int, plugin: Plugin): super(plugin){
        this.adapter = adapter()
        adapterCreator = adapter
        this.itemsOnePage = itemsOnePage
        this.title = title
        inventory = Bukkit.createInventory(null,itemsOnePage,"$title[$id]")
        page(0)
    }
    companion object {
        var id: Int = 0
    }
    abstract class Adapter{
        abstract val size: Int
        open val hasToolbar: Boolean = false
        abstract fun getItem(index: Int,currentPage: Int): ItemStack
        open fun getToolbarItem(index: Int): ItemStack{
            return ItemStack(Material.AIR)
        }
    }
    open val id = Companion.id++
    var currentPage: Int = 0
        private set
    lateinit var itemNextPage: ItemStack
    lateinit var itemPeriodPage: ItemStack
    fun refresh(){
        if (adapterCreator != null)
            adapter = adapterCreator.invoke()
        page(currentPage)
    }
    fun refresh(index: Int){
        if (index < adapter.size){
            val pageIndex = index - currentPage*(itemsOnePage-9)
            if (pageIndex in 0 .. itemsOnePage-9)
                inventory.setItem(pageIndex,adapter.getItem(index,currentPage))
        }
    }
    private fun page(index: Int){
        currentPage = index
        inventory.clear()
        val topExcepted = (index+1)*(itemsOnePage-9)
        for (i in index*(itemsOnePage-9) until if (adapter.size < topExcepted) adapter.size else topExcepted){
            inventory.setItem(
                    i,
                    adapter.getItem(i,index)
            )
        }
        inventory.setItem(itemsOnePage-9,
                ItemStack(Material.NETHER_STAR).also {
                    it.itemMeta = it.itemMeta!!.apply { setDisplayName(TextUtil.info("上一页")) }
                    itemPeriodPage = it
                }
        )
        inventory.setItem(itemsOnePage-1,
                ItemStack(Material.NETHER_STAR).also {
                    it.itemMeta = it.itemMeta!!.apply { setDisplayName(TextUtil.info("下一页")) }
                    itemNextPage = it
                }
        )
        if (adapter.hasToolbar){
            for (i in 8 downTo 2){
                inventory.setItem(itemsOnePage-i,
                        adapter.getToolbarItem(8-i))
            }
        }
    }

    private var l1 :((index: Int, item: ItemStack) -> Unit)? = null
    private var l2 :((index: Int, item: ItemStack) -> Unit)? = null
    fun setOnItemClickListener(l: (index: Int,item: ItemStack) -> Unit){
        this.l1 = l
    }
    fun setOnToolbarItemClickListener(l: (index: Int,item: ItemStack) -> Unit){
        this.l2 = l
    }

    final override fun onClick(event: InventoryClickEvent) {
        when (event.currentItem){
            itemPeriodPage -> {
                if (currentPage > 0){
                    currentPage--
                    page(currentPage)
                }
            }
            itemNextPage -> {
                if ((currentPage+1)*itemsOnePage < adapter.size){
                    currentPage++
                    page(currentPage)
                }
            }
            else -> {
                val index = event.rawSlot + currentPage * (itemsOnePage - 9)
                if (index < adapter.size)
                    l1?.invoke(index, event.currentItem!!)
                else if (event.rawSlot in itemsOnePage-8 .. itemsOnePage-2){
                    l2?.invoke(event.rawSlot - itemsOnePage + 8,event.currentItem!!)
                }
            }
        }
    }
}
package com.cvtracker.vmd.custom

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cvtracker.vmd.R
import com.cvtracker.vmd.data.Bookmark
import com.cvtracker.vmd.data.DisplayItem
import com.cvtracker.vmd.extensions.colorAttr
import com.cvtracker.vmd.extensions.hide
import com.cvtracker.vmd.extensions.show
import com.cvtracker.vmd.master.PrefHelper
import kotlinx.android.synthetic.main.item_available_center_header.view.*
import kotlinx.android.synthetic.main.item_center.view.*
import kotlinx.android.synthetic.main.item_last_updated.view.*
import kotlinx.android.synthetic.main.item_unavailable_center_header.view.*
import java.util.*

class CenterAdapter(
    private val context: Context,
    private val items: List<DisplayItem>,
    private val onClicked: (DisplayItem.Center) -> Unit,
    private val onBookmarkClicked: (DisplayItem.Center, Int) -> Unit,
    private val onAddressClicked: (String) -> Unit,
    private val onPhoneClicked: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mExpandedPosition = -1

    companion object {
        const val TYPE_CENTER = 0
        const val TYPE_CENTER_UNAVAILABLE = 1
        const val TYPE_AVAILABLE_HEADER = 3
        const val TYPE_UNAVAILABLE_HEADER = 4
        const val TYPE_LAST_UPDATED = 5
    }

    open inner class CenterViewHolder(
        context: Context,
        parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(context).inflate(R.layout.item_center, parent, false)
    ) {
        fun bind(center: DisplayItem.Center, position: Int) {
            with(itemView) {
                centerNameView.text = center.name

                dateView.text = when {
                    center.available && center.url.isNotBlank() && center.nextSlot != null -> center.formattedNextSlot
                    center.available && center.appointmentByPhoneOnly -> context.getString(R.string.appointment_by_phone_only)
                    else -> context.getString(R.string.no_slots_available)
                } + center.formattedDistance

                center.metadata?.address?.let { address ->
                    centerAddressView.text = center.formattedAddress
                    centerAddressView.show()
                    centerAddressView.setOnClickListener { onAddressClicked(address) }
                } ?: run {
                    centerAddressView.hide()
                    centerAddressView.setOnClickListener(null)
                }

                center.vaccineType?.let { vaccine ->
                    centerVaccineView.text = vaccine.joinToString(separator = " | ")
                    iconVaccineView.show()
                    centerVaccineView.show()
                } ?: run {
                    iconVaccineView.hide()
                    centerVaccineView.hide()
                }

                center.platformEnum?.let { partner ->
                    partnerImageView.setImageResource(partner.logo)
                    bottomSeparatorView.show()
                    partnerImageView.show()
                } ?: run {
                    bottomSeparatorView.hide()
                    partnerImageView.hide()
                }

                setupExpandedState(this, center, position)

                bookButton.setOnClickListener { onClicked.invoke(center) }
                checkButton.setOnClickListener { onClicked.invoke(center) }
                callButton.setOnClickListener { center.metadata?.phoneFormatted?.let { onPhoneClicked.invoke(it) } }
                bookmarkView.setOnClickListener { onBookmarkClicked.invoke(center, position) }

                appointmentsCountView.text =
                    String.format(
                        context.resources.getQuantityString(
                            R.plurals.shot_disponibilities,
                            center.appointmentCount, center.appointmentCount
                        )
                    )

                bookmarkView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
                    when (center.bookmark) {
                        Bookmark.NOTIFICATION -> R.drawable.ic_notifications_24dp
                        Bookmark.FAVORITE -> R.drawable.ic_bookmark_24dp
                        else -> R.drawable.ic_bookmark_border_24_dp
                    }, 0)
                bookmarkView.setText(
                    when{
                        center.available -> R.string.empty_string
                        center.bookmark != Bookmark.NOTIFICATION -> R.string.activate_notifs
                        else -> R.string.notifications_activated
                    }
                )

                if (center.available && center.appointmentByPhoneOnly) {
                    centreAvailableSpecificViews.hide()
                    callButton.text = context.getString(R.string.call_center, center.metadata?.phoneFormatted)
                    callButton.show()
                    checkButton.hide()
                    bookmarkView.hide()
                } else if (center.available) {
                    cardView.setCardBackgroundColor(colorAttr(R.attr.backgroundCardColor))
                    centreAvailableSpecificViews.show()
                    callButton.hide()
                    checkButton.hide()
                    bookmarkView.show()
                } else {
                    cardView.setCardBackgroundColor(colorAttr(R.attr.backgroundCardColorSecondary))
                    centreAvailableSpecificViews.hide()
                    checkButton.show()
                    callButton.hide()
                    bookmarkView.show()
                }
            }
        }
    }

    private fun setupExpandedState(itemView: View, center: DisplayItem.Center, position: Int) {
        with(itemView) {
            if (mExpandedPosition == position) {
                moreView.rotation = 180f

                center.typeLabel?.let { type ->
                    centerTypeView.text = type
                    centerTypeView.show()
                    iconTypeView.show()
                } ?: run {
                    centerTypeView.hide()
                    iconTypeView.hide()
                }

                center.metadata?.phoneFormatted?.let { phoneNumber ->
                    phoneView.setOnClickListener { onPhoneClicked(phoneNumber) }
                    phoneView.show()
                    phoneView.text = phoneNumber
                    iconPhoneView.show()
                } ?: run {
                    phoneView.hide()
                    iconPhoneView.hide()
                }

                center.metadata?.businessHours?.description?.let { hours ->
                    businessHoursView.show()
                    businessHoursView.text = hours
                    iconBusinessHoursView.show()
                } ?: run {
                    businessHoursView.hide()
                    iconBusinessHoursView.hide()
                }
            } else {
                moreView.rotation = 0f
                centerTypeView.hide()
                iconTypeView.hide()
                businessHoursView.hide()
                iconBusinessHoursView.hide()
                phoneView.hide()
                iconPhoneView.hide()
            }

            if (center.hasMoreInfoToShow) {
                moreView.show()
            } else {
                moreView.hide()
            }

            moreView.setOnClickListener {
                mExpandedPosition = if (mExpandedPosition == position) {
                    -1
                } else {
                    val oldPosition = mExpandedPosition
                    if (oldPosition >= 0) {
                        notifyItemChanged(oldPosition)
                    }
                    position
                }
                notifyItemChanged(position)
            }
        }
    }

    inner class UnavailableCenterHeaderViewHolder(context: Context, parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_unavailable_center_header, parent, false)
        ) {
        fun bind(header: DisplayItem.UnavailableCenterHeader) {
            with(itemView) {
                sectionLibelleView.setText(
                    when {
                        header.hasAvailableCenters -> R.string.no_slots_available_center_header_others
                        else -> R.string.no_slots_available_center_header
                    }
                )
            }
        }
    }

    inner class LastUpdatedViewHolder(context: Context, parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_last_updated, parent, false)
        ) {
        fun bind(item: DisplayItem.LastUpdated) {
            with(itemView) {
                lastUpdated.text = context.getString(
                    R.string.last_updated,
                    DateUtils.getRelativeTimeSpanString(
                        item.date.time,
                        System.currentTimeMillis(),
                        0L
                    )
                )
            }
        }
    }

    inner class AvailableCenterHeaderViewHolder(context: Context, parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_available_center_header, parent, false)
        ) {
        fun bind(header: DisplayItem.AvailableCenterHeader) {
            itemView.nbCenterView.text = header.placesCount.toString()
            itemView.libelleCenterAvailable.text =
                String.format(
                    context.resources.getQuantityString(
                        R.plurals.center_disponibilities,
                        header.placesCount, header.placesCount
                    )
                )
            itemView.nbAppointementView.text = header.slotsCount.toString()
            itemView.libelleAppointementAvailable.text =
                String.format(
                    context.resources.getQuantityString(
                        R.plurals.slot_disponibilities,
                        header.slotsCount, header.slotsCount
                    )
                )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CENTER -> CenterViewHolder(context, parent)
            TYPE_CENTER_UNAVAILABLE -> CenterViewHolder(context, parent)
            TYPE_UNAVAILABLE_HEADER -> UnavailableCenterHeaderViewHolder(context, parent)
            TYPE_AVAILABLE_HEADER -> AvailableCenterHeaderViewHolder(context, parent)
            TYPE_LAST_UPDATED -> LastUpdatedViewHolder(context, parent)
            else -> throw IllegalArgumentException("Type not supported")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val result = items[position]) {
            is DisplayItem.Center -> {
                if (result.available) {
                    TYPE_CENTER
                } else {
                    TYPE_CENTER_UNAVAILABLE
                }
            }
            is DisplayItem.UnavailableCenterHeader -> TYPE_UNAVAILABLE_HEADER
            is DisplayItem.AvailableCenterHeader -> TYPE_AVAILABLE_HEADER
            is DisplayItem.LastUpdated -> TYPE_LAST_UPDATED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CenterViewHolder -> {
                holder.bind(items[position] as DisplayItem.Center, position)
            }
            is UnavailableCenterHeaderViewHolder -> {
                holder.bind(items[position] as DisplayItem.UnavailableCenterHeader)
            }
            is AvailableCenterHeaderViewHolder -> {
                holder.bind(items[position] as DisplayItem.AvailableCenterHeader)
            }
            is LastUpdatedViewHolder -> {
                holder.bind(items[position] as DisplayItem.LastUpdated)
            }
        }
    }

    override fun getItemCount() = items.size

    fun refreshBookmarkState() {
        val centersBookmark = PrefHelper.centersBookmark
        items.filterIsInstance<DisplayItem.Center>().onEach { center ->
            center.bookmark = centersBookmark
                .firstOrNull { center.id == it.centerId }?.bookmark
                ?: Bookmark.NONE
        }
        notifyDataSetChanged()
    }

}
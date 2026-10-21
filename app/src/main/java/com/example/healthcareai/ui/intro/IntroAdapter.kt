package com.example.healthcareai.ui.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.databinding.ItemIntroSlideBinding

data class IntroSlide(
    val title: String,
    val description: String,
    @param:DrawableRes val illustrationRes: Int
)

class IntroAdapter(
    private val slides: List<IntroSlide>
) : RecyclerView.Adapter<IntroAdapter.SlideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val binding = ItemIntroSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SlideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    inner class SlideViewHolder(
        private val binding: ItemIntroSlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: IntroSlide) {
            binding.tvTitle.text = slide.title
            binding.tvDescription.text = slide.description
            binding.ivIllustration.setImageResource(slide.illustrationRes)
        }
    }
}

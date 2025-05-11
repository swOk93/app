package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка кнопки добавления привычки
        binding.addButton.setOnClickListener {
            val habitText: String = binding.habitEditText.text.toString()
            if (habitText.isNotEmpty()) {
                // Здесь будет логика добавления привычки
                Toast.makeText(this, "Привычка добавлена: $habitText", Toast.LENGTH_SHORT).show()
                binding.habitEditText.text.clear()
            } else {
                Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Настройка кнопок типов привычек
        binding.timeButton.setOnClickListener {
            Toast.makeText(this, "Выбран тип: Время", Toast.LENGTH_SHORT).show()
        }
        
        binding.repeatButton.setOnClickListener {
            Toast.makeText(this, "Выбран тип: Повторения", Toast.LENGTH_SHORT).show()
        }
        
        binding.simpleButton.setOnClickListener {
            Toast.makeText(this, "Выбран тип: Простая", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
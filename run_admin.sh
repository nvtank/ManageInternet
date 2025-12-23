#!/bin/bash

echo "=== Biên dịch và chạy Admin Application ==="
echo ""

cd /home/nvtank/year3/ki1/LTM/ManageInternet/Admin/src

echo "Bước 1: Biên dịch..."
javac -cp ".:library/*:library/JFreeChart/*" Main.java

if [ $? -eq 0 ]; then
    echo "✓ Biên dịch thành công!"
    echo ""
    echo "Bước 2: Chạy ứng dụng..."
    java -cp ".:library/*:library/JFreeChart/*" Main
else
    echo "✗ Lỗi biên dịch!"
    exit 1
fi

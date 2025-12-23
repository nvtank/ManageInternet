#!/bin/bash

echo "=== Biên dịch và chạy User Application ==="
echo ""

cd /home/nvtank/year3/ki1/LTM/ManageInternet/User/src

echo "Bước 1: Biên dịch..."
javac -cp ".:library/*" Main.java

if [ $? -eq 0 ]; then
    echo "✓ Biên dịch thành công!"
    echo ""
    echo "Bước 2: Chạy ứng dụng..."
    java -cp ".:library/*" Main
else
    echo "✗ Lỗi biên dịch!"
    exit 1
fi
